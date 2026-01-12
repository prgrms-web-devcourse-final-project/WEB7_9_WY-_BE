import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// ---- Metrics ----
export const ok200 = new Counter('hold_200_total');
export const ok409 = new Counter('hold_409_total');
export const bad4xx = new Counter('hold_bad_4xx_total');
export const bad5xx = new Counter('hold_bad_5xx_total');
export const setupFail = new Rate('setup_failed');

// ---- Env ----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCHEDULE_ID = String(__ENV.SCHEDULE_ID || '3');
const TOKEN = __ENV.TOKEN;                              // "Bearer xxx"
const DEVICE_ID = __ENV.DEVICE_ID || 'Device-123';
const SEAT_IDS = (__ENV.SEAT_IDS || '30001').split(',').map((v) => Number(v.trim()));
const ARRIVAL = Number(__ENV.ARRIVAL || '30');         // 30 → 50 → 80 → 120
const TEST_DURATION_SEC = Number(__ENV.DURATION_SEC || '50');
const RAMP_SEC = Number(__ENV.RAMP_SEC || '5');

if (!TOKEN) throw new Error('TOKEN env is required. e.g. -e TOKEN="Bearer xxx"');

// ---- Options ----
export const options = {
    scenarios: {
        hotseat_atomicity: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: Math.max(60, ARRIVAL * 2),
            maxVUs: Math.max(300, ARRIVAL * 10),
            stages: [
                { target: ARRIVAL, duration: `${RAMP_SEC}s` },
                { target: ARRIVAL, duration: `${TEST_DURATION_SEC}s` },
                { target: 0, duration: `${RAMP_SEC}s` },
            ],
            gracefulStop: '10s',
        },
    },
    thresholds: {
        setup_failed: ['rate==0'],
    },
};

// ---- Helpers ----
function authHeaders(extra = {}) {
    return {
        headers: {
            Authorization: TOKEN,
            Accept: 'application/json',
            ...extra,
        },
    };
}

function jsonHeaders(extra = {}) {
    return authHeaders({ 'Content-Type': 'application/json', ...extra });
}

// ---- Setup: queue join -> booking-session create -> createReservation ----
export function setup() {
    // 1) Queue join
    const joinUrl = `${BASE_URL}/api/v1/queue/join/${SCHEDULE_ID}`;
    const joinRes = http.post(joinUrl, '', authHeaders({ 'X-Device-Id': DEVICE_ID }));

    const joinOk = check(joinRes, {
        'setup queue join 200': (r) => r.status === 200,
    });

    if (!joinOk) {
        setupFail.add(true);
        console.log(`[SETUP] queue join failed status=${joinRes.status} body=${joinRes.body}`);
        throw new Error('setup failed: queue join');
    }

    const joinBody = JSON.parse(joinRes.body);
    const waitingToken = joinBody.waitingToken;

    if (!waitingToken) {
        setupFail.add(true);
        console.log(`[SETUP] waitingToken missing body=${joinRes.body}`);
        throw new Error('setup failed: missing waitingToken');
    }

    // 2) BookingSession create
    const bsCreateUrl = `${BASE_URL}/api/v1/booking-session/create`;
    const bsPayload = JSON.stringify({
        scheduleId: Number(SCHEDULE_ID),
        waitingToken,
        deviceId: DEVICE_ID,
    });

    const bsRes = http.post(bsCreateUrl, bsPayload, jsonHeaders());

    const bsOk = check(bsRes, {
        'setup booking-session create 200': (r) => r.status === 200,
    });

    if (!bsOk) {
        setupFail.add(true);
        console.log(`[SETUP] booking-session create failed status=${bsRes.status} body=${bsRes.body}`);
        throw new Error('setup failed: booking-session create');
    }

    const bsBody = JSON.parse(bsRes.body);
    const bookingSessionId = bsBody.bookingSessionId;

    if (!bookingSessionId) {
        setupFail.add(true);
        console.log(`[SETUP] bookingSessionId missing body=${bsRes.body}`);
        throw new Error('setup failed: missing bookingSessionId');
    }

    // 3) Create reservation (requires X-BOOKING-SESSION-ID)
    const createResUrl = `${BASE_URL}/api/v1/booking/schedule/${SCHEDULE_ID}/reservation`;
    const createResRes = http.post(createResUrl, '', authHeaders({ 'X-BOOKING-SESSION-ID': bookingSessionId }));

    const crOk = check(createResRes, {
        'setup createReservation 200': (r) => r.status === 200,
    });

    if (!crOk) {
        setupFail.add(true);
        console.log(`[SETUP] createReservation failed status=${createResRes.status} body=${createResRes.body}`);
        throw new Error('setup failed: createReservation');
    }

    const crBody = JSON.parse(createResRes.body);
    const reservationId = crBody.reservationId;

    if (!reservationId) {
        setupFail.add(true);
        console.log(`[SETUP] reservationId missing body=${createResRes.body}`);
        throw new Error('setup failed: missing reservationId');
    }

    return { bookingSessionId, reservationId };
}

// ---- Main: HOLD only (atomicity) ----
export default function (data) {
    const { bookingSessionId, reservationId } = data;

    const holdUrl = `${BASE_URL}/api/v1/booking/reservation/${reservationId}/seats:hold`;
    const payload = JSON.stringify({ performanceSeatIds: SEAT_IDS });

    const res = http.post(holdUrl, payload, jsonHeaders({ 'X-BOOKING-SESSION-ID': bookingSessionId }));
    if (res.status !== 200 && res.status !== 409) {
        console.log(`[HOLD] status=${res.status} body=${res.body}`);
    }

    // 200/409는 정상 (승리/패배)
    if (res.status === 200) ok200.add(1);
    else if (res.status === 409) ok409.add(1);
    else if (res.status >= 400 && res.status < 500) bad4xx.add(1);
    else if (res.status >= 500) bad5xx.add(1);

    check(res, {
        'hold success(200) or expected conflict(409)': (r) => r.status === 200 || r.status === 409,
        'no 5xx': (r) => r.status < 500,
    });

    sleep(0.001);
}
