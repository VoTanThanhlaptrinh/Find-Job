package web_application;

import com.job_web.service.support.impl.S3PresignedUrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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
    void setUp() throws MalformedURLException {
        ReflectionTestUtils.setField(s3PresignedUrlService, "bucketName", BUCKET_NAME);
        when(presignedGetObjectRequest.url()).thenReturn(new URL(TEST_PRESIGNED_URL));
    }

    @Nested
    @DisplayName("generateViewUrl Tests")
    class GenerateViewUrlTests {

        @Test
        @DisplayName("S01: Tạo view URL thành công")
        void generateViewUrl_Success() {
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
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            String result = s3PresignedUrlService.generateDownloadUrl(TEST_KEY, null, EXPIRATION_MINUTES);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Tạo URL với different file extensions")
        void generateUrl_WithDifferentFileExtensions() {
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
}
