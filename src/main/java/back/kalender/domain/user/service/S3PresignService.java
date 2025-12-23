package back.kalender.domain.user.service;

import back.kalender.domain.user.dto.response.PresignProfileImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.profile-prefix:profile/}")
    private String profilePrefix;

    @Value("${aws.s3.presign-expire-minutes:10}")
    private long presignExpireMinutes;

    public PresignProfileImageResponse presignProfileImagePut(Long userId, String contentType) {
        String key = profilePrefix + userId + "/" + UUID.randomUUID();

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpireMinutes))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        return new PresignProfileImageResponse(
                key,
                presigned.url().toString(),
                Map.of("Content-Type", contentType),
                presignExpireMinutes * 60
        );
    }

    public String presignProfileImageGet(String key) {
        if (key == null || key.isBlank()) return null;

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpireMinutes))
                .getObjectRequest(getReq)
                .build();

        return s3Presigner.presignGetObject(presignReq).url().toString();
    }
}
