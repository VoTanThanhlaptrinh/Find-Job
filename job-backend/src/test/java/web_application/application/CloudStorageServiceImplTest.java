package web_application.application;

import com.nlu.shared.application.impl.CloudStorageServiceImpl;
import com.nlu.shared.domain.exception.StorageException;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private CloudStorageServiceImpl cloudStorageService;

    private static final String BUCKET_NAME = "test-r2-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cloudStorageService, "bucketName", BUCKET_NAME);
    }

    @Test
    @DisplayName("headObject trả về metadata hợp lệ khi object tồn tại và không tải nội dung")
    void headObject_Success() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(1024L)
                .contentType("application/pdf")
                .eTag("\"abc123etag\"")
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        Optional<StorageObjectMetadata> result = cloudStorageService.headObject("temp/resumes/1/uuid");

        assertTrue(result.isPresent());
        assertEquals("temp/resumes/1/uuid", result.get().key());
        assertEquals(1024L, result.get().contentLength());
        assertEquals("application/pdf", result.get().contentType());
        assertEquals("\"abc123etag\"", result.get().etag());

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue().bucket());
        assertEquals("temp/resumes/1/uuid", captor.getValue().key());
    }

    @Test
    @DisplayName("headObject trả về Optional.empty() khi NoSuchKeyException xảy ra (object không tồn tại)")
    void headObject_ReturnsEmpty_WhenNoSuchKeyException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        Optional<StorageObjectMetadata> result = cloudStorageService.headObject("nonexistent-key");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("headObject trả về Optional.empty() khi S3Exception với status 404")
    void headObject_ReturnsEmpty_WhenS3Exception404() {
        S3Exception s3Exception404 = (S3Exception) S3Exception.builder()
                .statusCode(404)
                .message("Not Found")
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception404);

        Optional<StorageObjectMetadata> result = cloudStorageService.headObject("nonexistent-key");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("headObject ném StorageException khi gặp lỗi kết nối storage (khác 404)")
    void headObject_ThrowsStorageException_WhenConnectionFails() {
        S3Exception s3Exception500 = (S3Exception) S3Exception.builder()
                .statusCode(500)
                .message("Internal Server Error")
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception500);

        assertThrows(StorageException.class, () ->
                cloudStorageService.headObject("some-key")
        );
    }

    @Test
    @DisplayName("copyObject thực hiện server-side copy với copySource được URL encode đúng chuẩn AWS SDK")
    void copyObject_Success() {
        String sourceKey = "temp/resumes/1/uuid-abc";
        String destinationKey = "resumes/1/uuid-abc";

        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());

        cloudStorageService.copyObject(sourceKey, destinationKey);

        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(captor.capture());

        CopyObjectRequest captured = captor.getValue();
        assertEquals(BUCKET_NAME, captured.destinationBucket());
        assertEquals(destinationKey, captured.destinationKey());
        // copySource must be bucket/encodedKey
        assertEquals(BUCKET_NAME + "/temp/resumes/1/uuid-abc", captured.copySource());
    }

    @Test
    @DisplayName("deleteObject gọi s3Client.deleteObject")
    void deleteObject_Success() {
        String key = "temp/resumes/1/uuid-abc";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        cloudStorageService.deleteObject(key);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue().bucket());
        assertEquals(key, captor.getValue().key());
    }
}
