import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";
import { randomIntBetween } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SCHEDULE_ID = __ENV.SCHEDULE_ID || "3";
const ARRIVAL = Number(__ENV.ARRIVAL || "30");
const THINK_TIME_MS = Number(__ENV.THINK_TIME_MS || "0");
const DEVICE_PREFIX = __ENV.DEVICE_PREFIX || "Device";

const TOKEN_CSV_PATH = __ENV.TOKEN_CSV || "./tokens.csv";
const SEAT_JSON_PATH = __ENV.SEAT_JSON || "./seat_ids.json";

const SEAT_PICK_MODE = (__ENV.SEAT_PICK_MODE || "random").toLowerCase(); // random | roundrobin
const QUEUE_STATUS_POLL_MS = Number(__ENV.QUEUE_STATUS_POLL_MS || "200"); // status polling interval
const QUEUE_MAX_WAIT_MS = Number(__ENV.QUEUE_MAX_WAIT_MS || "15000"); // max wait to get waitingToken
const DO_QUEUE_PING = (__ENV.DO_QUEUE_PING || "true") === "true"; // queue/ping 호출할지
const DO_BOOKING_PING = (__ENV.DO_BOOKING_PING || "false") === "true"; // booking-session/ping 호출할지

// -------------------- data loaders --------------------
function parseCsvTokens(text) {
    const lines = text.trim().split("\n");
    if (lines.length < 2) throw new Error("tokens.csv must have header + at least 1 row");

    const header = lines[0].split(",").map((s) => s.trim());
    const tokenIdx = header.indexOf("token");
    if (tokenIdx === -1) throw new Error('tokens.csv must have header column named "token"');

    const tokens = [];
    for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(",").map((s) => s.trim());
        if (!cols[tokenIdx]) continue;
        tokens.push(cols[tokenIdx]);
    }
    if (tokens.length === 0) throw new Error("tokens.csv has no tokens");
    return tokens;
}

const TOKENS = new SharedArray("TOKENS", () => {
    const csvText = open(TOKEN_CSV_PATH);
    return parseCsvTokens(csvText);
});

const SEAT_IDS = new SharedArray("SEAT_IDS", () => {
    const raw = JSON.parse(open(SEAT_JSON_PATH));
    if (!raw || !Array.isArray(raw.performanceSeatIds)) {
        throw new Error('seat_ids.json must contain { "performanceSeatIds": [ ... ] }');
    }
    if (raw.performanceSeatIds.length === 0) {
        throw new Error("seat_ids.json performanceSeatIds is empty");
    }
    return raw.performanceSeatIds;
});

// -------------------- k6 options --------------------
export const options = {
    scenarios: {
        integrated_compete: {
            executor: "ramping-arrival-rate",
            startRate: ARRIVAL,
            timeUnit: "1s",
            preAllocatedVUs: 200,
            maxVUs: 1200,
            stages: [
                { target: ARRIVAL, duration: "20s" },
                { target: ARRIVAL, duration: "40s" },
                { target: ARRIVAL, duration: "20s" },
            ],
            gracefulStop: "10s",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.2"],
    },
};

// -------------------- helpers --------------------
function pickToken(vuId) {
    return TOKENS[(vuId - 1) % TOKENS.length];
}

function makeDeviceId(vuId) {
    const idx = (vuId - 1) % TOKENS.length;
    return `${DEVICE_PREFIX}-${idx + 1}`;
}

let seatCursor = 0;
function pickSeatId() {
    if (SEAT_PICK_MODE === "roundrobin") {
        const id = SEAT_IDS[seatCursor % SEAT_IDS.length];
        seatCursor++;
        return id;
    }
    return SEAT_IDS[randomIntBetween(0, SEAT_IDS.length - 1)];
}

function authHeaders(token, extra = {}) {
    return {
        headers: {
            accept: "application/json",
            "content-type": "application/json",
            Authorization: token,
            ...extra,
        },
    };
}

// -------------------- API calls --------------------
function queueJoin(scheduleId, deviceId, token) {
    const url = `${BASE_URL}/api/v1/queue/join/${scheduleId}`;
    return http.post(url, "", {
        headers: {
            accept: "application/json",
            "X-Device-Id": deviceId,
            Authorization: token,
        },
    });
}

function queueStatus(scheduleId, qsid, token) {
    const url = `${BASE_URL}/api/v1/queue/status/${scheduleId}`;
    return http.get(url, {
        headers: {
            accept: "application/json",
            "X-QSID": qsid,
            Authorization: token,
        },
    });
}

function queuePing(scheduleId, qsid, token) {
    const url = `${BASE_URL}/api/v1/queue/ping/${scheduleId}`;
    return http.post(url, "", {
        headers: {
            accept: "application/json",
            "X-QSID": qsid,
            Authorization: token,
        },
    });
}

function bookingSessionCreate(scheduleId, waitingToken, deviceId, token) {
    const url = `${BASE_URL}/api/v1/booking-session/create`;
    const body = JSON.stringify({
        scheduleId: Number(scheduleId),
        waitingToken: waitingToken,
        deviceId: deviceId,
    });
    return http.post(url, body, authHeaders(token));
}

function bookingSessionPing(scheduleId, bookingSessionId, token) {
    const url = `${BASE_URL}/api/v1/booking-session/ping/${scheduleId}`;
    return http.post(
        url,
        "",
        authHeaders(token, { "X-BOOKING-SESSION-ID": bookingSessionId })
    );
}

function createReservation(scheduleId, bookingSessionId, token) {
    const url = `${BASE_URL}/api/v1/booking/schedule/${scheduleId}/reservation`;
    return http.post(url, "", authHeaders(token, { "X-BOOKING-SESSION-ID": bookingSessionId }));
}

function holdSeats(reservationId, bookingSessionId, token, performanceSeatId) {
    const url = `${BASE_URL}/api/v1/booking/reservation/${reservationId}/seats:hold`;
    const body = JSON.stringify({
        performanceSeatIds: [Number(performanceSeatId)],
    });

    return http.post(
        url,
        body,
        authHeaders(token, { "X-BOOKING-SESSION-ID": bookingSessionId })
    );
}

// -------------------- main flow --------------------
function getWaitingTokenOrFail(scheduleId, deviceId, token) {
    const joinRes = queueJoin(scheduleId, deviceId, token);
    if (!check(joinRes, { "queue join 200": (r) => r.status === 200 })) {
        console.log(`[JOIN FAIL] status=${joinRes.status} body=${joinRes.body}`);
        return null;
    }

    const joinBody = joinRes.json();
    const qsid = joinBody.qsid;
    if (!qsid) {
        console.log(`[JOIN PARSE FAIL] body=${joinRes.body}`);
        return null;
    }

    if (joinBody.status === "ADMITTED" && joinBody.waitingToken) {
        return { qsid, waitingToken: joinBody.waitingToken };
    }

    const start = Date.now();
    while (Date.now() - start < QUEUE_MAX_WAIT_MS) {
        if (DO_QUEUE_PING) queuePing(scheduleId, qsid, token);

        const st = queueStatus(scheduleId, qsid, token);
        if (st.status !== 200) {
            console.log(`[STATUS FAIL] status=${st.status} body=${st.body}`);
            sleep(QUEUE_STATUS_POLL_MS / 1000);
            continue;
        }

        const body = st.json();
        if (body.status === "ADMITTED" && body.waitingToken) {
            return { qsid, waitingToken: body.waitingToken };
        }

        sleep(QUEUE_STATUS_POLL_MS / 1000);
    }

    console.log(`[QUEUE TIMEOUT] could not get waitingToken within ${QUEUE_MAX_WAIT_MS}ms`);
    return null;
}

export default function () {
    const vuId = __VU;

    const token = pickToken(vuId);
    const deviceId = makeDeviceId(vuId);

    const qt = getWaitingTokenOrFail(SCHEDULE_ID, deviceId, token);
    if (!qt) return;

    const bsRes = bookingSessionCreate(SCHEDULE_ID, qt.waitingToken, deviceId, token);
    if (!check(bsRes, { "booking-session create 200": (r) => r.status === 200 })) {
        console.log(`[BS FAIL] status=${bsRes.status} body=${bsRes.body}`);
        return;
    }

    const bookingSessionId = bsRes.json().bookingSessionId;
    if (!bookingSessionId) {
        console.log(`[BS PARSE FAIL] body=${bsRes.body}`);
        return;
    }

    if (DO_BOOKING_PING) bookingSessionPing(SCHEDULE_ID, bookingSessionId, token);

    const rRes = createReservation(SCHEDULE_ID, bookingSessionId, token);
    if (!check(rRes, { "createReservation 200": (r) => r.status === 200 })) {
        console.log(`[RESERVATION FAIL] status=${rRes.status} body=${rRes.body} bookingSessionId=${bookingSessionId}`);
        return;
    }

    const reservationId = rRes.json().reservationId;
    if (!reservationId) {
        console.log(`[RESERVATION PARSE FAIL] body=${rRes.body}`);
        return;
    }

    const seatId = pickSeatId();
    const hRes = holdSeats(reservationId, bookingSessionId, token, seatId);

    const ok = check(hRes, {
        "hold expected (200 or 409)": (r) => r.status === 200 || r.status === 409,
        "no 5xx": (r) => r.status < 500,
    });

    if (!ok) {
        console.log(`[HOLD UNEXPECTED] status=${hRes.status} body=${hRes.body} seatId=${seatId} reservationId=${reservationId}`);
    }

    if (THINK_TIME_MS > 0) sleep(THINK_TIME_MS / 1000);
}
