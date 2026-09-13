package web_application;

import com.nlu.shared.application.impl.S3PresignedUrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho S3PresignedUrlService.
 * 
 * Test cases bao gồm:
 * - Tạo view URL thành công
 * - Tạo download URL thành công
 * - Xử lý file name có ký tự đặc biệt
 * - Error handling khi S3Presigner gặp lỗi
 */
@ExtendWith(MockitoExtension.class)
class S3PresignedUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    @InjectMocks
    private S3PresignedUrlServiceImpl s3PresignedUrlService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_KEY = "resume-key-123";
    private static final String TEST_FILE_NAME = "resume.pdf";
    private static final String TEST_PRESIGNED_URL = "https://bucket.r2.cloudflarestorage.com/resume.pdf?X-Amz-Signature=abc123";
    private static final int EXPIRATION_MINUTES = 30;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3PresignedUrlService, "bucketName", BUCKET_NAME);
    }

    private void mockPresignedUrl() {
        try {
            when(presignedGetObjectRequest.url()).thenReturn(new URL(TEST_PRESIGNED_URL));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("generateViewUrl Tests")
    class GenerateViewUrlTests {

        @Test
        @DisplayName("S01: Tạo view URL thành công")
        void generateViewUrl_Success() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateViewUrl(TEST_KEY, EXPIRATION_MINUTES);

            assertNotNull(result);
            assertEquals(TEST_PRESIGNED_URL, result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("S03: Throw RuntimeException khi S3Presigner gặp lỗi")
        void generateViewUrl_ThrowsException_WhenPresignerFails() {
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("S3 connection failed"));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                s3PresignedUrlService.generateViewUrl(TEST_KEY, EXPIRATION_MINUTES)
            );

            assertTrue(exception.getMessage().contains("Failed to generate view URL"));
        }

        @Test
        @DisplayName("Tạo URL với key rỗng vẫn gọi S3Presigner")
        void generateViewUrl_WithEmptyKey() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateViewUrl("", EXPIRATION_MINUTES);

            assertNotNull(result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }
    }

    @Nested
    @DisplayName("generateDownloadUrl Tests")
    class GenerateDownloadUrlTests {

        @Test
        @DisplayName("S02: Tạo download URL thành công")
        void generateDownloadUrl_Success() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateDownloadUrl(TEST_KEY, TEST_FILE_NAME, EXPIRATION_MINUTES);

            assertNotNull(result);
            assertEquals(TEST_PRESIGNED_URL, result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("S04: Tạo URL với filename chứa ký tự đặc biệt")
        void generateDownloadUrl_WithSpecialCharacters() {
            mockPresignedUrl();
            String specialFileName = "CV Nguyễn Văn A (2024).pdf";
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateDownloadUrl(TEST_KEY, specialFileName, EXPIRATION_MINUTES);

            assertNotNull(result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("Tạo URL với filename chứa khoảng trắng")
        void generateDownloadUrl_WithSpaces() {
            mockPresignedUrl();
            String fileNameWithSpaces = "my resume document.pdf";
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateDownloadUrl(TEST_KEY, fileNameWithSpaces, EXPIRATION_MINUTES);

            assertNotNull(result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("Throw RuntimeException khi S3Presigner gặp lỗi")
        void generateDownloadUrl_ThrowsException_WhenPresignerFails() {
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("Network error"));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                s3PresignedUrlService.generateDownloadUrl(TEST_KEY, TEST_FILE_NAME, EXPIRATION_MINUTES)
            );

            assertTrue(exception.getMessage().contains("Failed to generate download URL"));
        }
    }

    @Nested
    @DisplayName("S05: Expiration Time Tests")
    class ExpirationTimeTests {

        @Test
        @DisplayName("Verify presign request được gọi với đúng expiration time")
        void verifyExpirationTimeIsUsed() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenAnswer(invocation -> {
                        GetObjectPresignRequest request = invocation.getArgument(0);
                        assertEquals(Duration.ofMinutes(EXPIRATION_MINUTES), request.signatureDuration());
                        return presignedGetObjectRequest;
                    });

            s3PresignedUrlService.generateViewUrl(TEST_KEY, EXPIRATION_MINUTES);

            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("Tạo URL với expiration time khác nhau")
        void generateUrl_WithDifferentExpirationTimes() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            // Test với 5 phút
            String result1 = s3PresignedUrlService.generateViewUrl(TEST_KEY, 5);
            assertNotNull(result1);

            // Test với 60 phút
            String result2 = s3PresignedUrlService.generateViewUrl(TEST_KEY, 60);
            assertNotNull(result2);

            verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Tạo URL với key chứa đường dẫn (path)")
        void generateUrl_WithPathInKey() {
            mockPresignedUrl();
            String keyWithPath = "users/123/resumes/resume.pdf";
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateViewUrl(keyWithPath, EXPIRATION_MINUTES);

            assertNotNull(result);
            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("Tạo URL với filename null")
        void generateDownloadUrl_WithNullFilename() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateDownloadUrl(TEST_KEY, null, EXPIRATION_MINUTES);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Tạo URL với different file extensions")
        void generateUrl_WithDifferentFileExtensions() {
            mockPresignedUrl();
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            // PDF
            assertNotNull(s3PresignedUrlService.generateDownloadUrl(TEST_KEY, "resume.pdf", EXPIRATION_MINUTES));
            
            // DOCX
            assertNotNull(s3PresignedUrlService.generateDownloadUrl(TEST_KEY, "resume.docx", EXPIRATION_MINUTES));
            
            // DOC
            assertNotNull(s3PresignedUrlService.generateDownloadUrl(TEST_KEY, "resume.doc", EXPIRATION_MINUTES));

            verify(s3Presigner, times(3)).presignGetObject(any(GetObjectPresignRequest.class));
        }
    }

    @Nested
    @DisplayName("generateUploadUrl Tests")
    class GenerateUploadUrlTests {

        @Test
        @DisplayName("Tạo presigned upload URL thành công cho HTTP PUT với đúng bucket, key, content type và metadata")
        void generateUploadUrl_Success() throws MalformedURLException {
            software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest presignedPut =
                    mock(software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest.class);
            when(presignedPut.url()).thenReturn(new URL("https://bucket.r2.cloudflarestorage.com/temp/resumes/1/abc?sig=xyz"));

            java.util.UUID uploadId = java.util.UUID.randomUUID();
            when(s3Presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                    .thenReturn(presignedPut);

            com.nlu.shared.domain.model.PresignedUploadUrlResponse response = s3PresignedUrlService.generateUploadUrl(
                    "temp/resumes/1/abc",
                    "application/pdf",
                    uploadId,
                    10
            );

            assertNotNull(response);
            assertEquals("https://bucket.r2.cloudflarestorage.com/temp/resumes/1/abc?sig=xyz", response.url());
            assertEquals("PUT", response.method());
            assertEquals("application/pdf", response.requiredHeaders().get("Content-Type"));
            assertNotNull(response.expiresAt());

            ArgumentCaptor<software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());
            software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest captured = captor.getValue();
            assertEquals(Duration.ofMinutes(10), captured.signatureDuration());
            assertEquals(BUCKET_NAME, captured.putObjectRequest().bucket());
            assertEquals("temp/resumes/1/abc", captured.putObjectRequest().key());
            assertEquals("application/pdf", captured.putObjectRequest().contentType());
            assertEquals(uploadId.toString(), captured.putObjectRequest().metadata().get("upload-id"));
        }

        @Test
        @DisplayName("Throw RuntimeException khi presignPutObject gặp lỗi")
        void generateUploadUrl_ThrowsException_WhenPresignerFails() {
            when(s3Presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("R2 sign error"));

            assertThrows(RuntimeException.class, () ->
                    s3PresignedUrlService.generateUploadUrl("temp/key", "application/pdf", java.util.UUID.randomUUID(), 10)
            );
        }
    }
}

