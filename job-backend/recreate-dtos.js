const fs = require('fs');
const path = require('path');

const files = [
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/auth/LoginDTO.java'),
    content: `package com.job_web.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "{validation.role.required}")
        String role,

        @NotBlank(message = "{validation.username.required}")
        String username,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/auth/RegistationForm.java'),
    content: `package com.job_web.dto.auth;

import com.job_web.constant.RoleConstants;
import com.job_web.custom.EmailExist;
import com.job_web.models.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

public record RegistationForm(
        @NotBlank(message = "{validation.fullName.required}")
        @Size(max = 255, message = "{validation.fullName.maxSize}")
        String fullName,

        @NotBlank(message = "{validation.username.required}")
        @Email(message = "{validation.email.invalid}")
        @EmailExist
        String username,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, message = "{validation.password.minSize}")
        String password,

        @NotBlank(message = "{validation.confirmPassword.required}")
        String confirmPassword
) {
    @AssertTrue(message = "{validation.password.mismatch}")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public User toUser(PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(username);
        user.setFullName(fullName);
        user.setAddress("");
        user.setRole(RoleConstants.ROLE_USER);
        user.setMobile("");
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/auth/ResetDTO.java'),
    content: `package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetDTO(
        @Size(min = 8, message = "{validation.password.minSize}")
        @NotBlank(message = "{validation.newPassword.required}")
        String newPass,

        @NotBlank(message = "{validation.confirmPassword.required}")
        String confirmPass,

        @NotBlank(message = "{validation.code.required}")
        String random
) {
    @AssertTrue(message = "{validation.password.mismatch}")
    public boolean isValid() {
        return newPass.equals(confirmPass);
    }

    public String getNewPass() {
        return newPass;
    }

    public String getConfirmPass() {
        return confirmPass;
    }

    public String getRandom() {
        return random;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/auth/ChangePassDTO.java'),
    content: `package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePassDTO(
        @NotNull(message = "{validation.oldPassword.required}")
        String oldPass,

        @NotNull(message = "{validation.newPassword.required}")
        @Size(min = 8, message = "{validation.password.minSize}")
        String newPass,

        @NotNull(message = "{validation.confirmPassword.required}")
        String confirmPass
) {
    @AssertTrue(message = "{validation.password.mismatch}")
    public boolean isPasswordsMatch() {
        return newPass != null && newPass.equals(confirmPass);
    }

    public String getOldPass() {
        return oldPass;
    }

    public String getNewPass() {
        return newPass;
    }

    public String getConfirmPass() {
        return confirmPass;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/user/UserCrudDTO.java'),
    content: `package com.job_web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCrudDTO(
        @NotBlank(message = "{validation.fullName.required}")
        @Size(max = 255, message = "{validation.fullName.maxSize}")
        String fullName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, message = "{validation.password.minSize}")
        String password,

        @NotBlank(message = "{validation.role.required}")
        String role,

        @NotNull(message = "{validation.dateOfBirth.required}")
        @Past(message = "{validation.dateOfBirth.past}")
        LocalDate dateOfBirth,

        @NotBlank(message = "{validation.address.required}")
        String address,

        @NotBlank(message = "{validation.mobile.required}")
        @Pattern(regexp = "^\\d{10}$", message = "{validation.mobile.invalid}")
        String mobile,

        @NotNull(message = "{validation.active.required}")
        Boolean active,

        @NotNull(message = "{validation.accountLocked.required}")
        Boolean accountLocked,

        @NotNull(message = "{validation.enabled.required}")
        Boolean enabled,

        @NotNull(message = "{validation.oauth2Enabled.required}")
        Boolean oauth2Enabled
) {
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getMobile() {
        return mobile;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getAccountLocked() {
        return accountLocked;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getOauth2Enabled() {
        return oauth2Enabled;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/profile/UserInfo.java'),
    content: `package com.job_web.dto.profile;

import com.job_web.models.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record UserInfo(
        @NotNull(message = "{validation.fullName.required}")
        String fullName,

        @NotNull(message = "{validation.dateOfBirth.required}")
        LocalDate dateOfBirth,

        @NotNull(message = "{validation.address.required}")
        String address,

        @NotNull(message = "{validation.mobile.required}")
        String mobile
) {
    @AssertTrue(message = "{validation.mobile.invalid}")
    public boolean isMobile() {
        Pattern pattern = Pattern.compile("^\\d{10}$");
        Matcher matcher = pattern.matcher(mobile);
        return matcher.matches();
    }

    public static UserInfo fromUser(User userLogin) {
        return new UserInfo(
                userLogin.getFullName(),
                userLogin.getDateOfBirth(),
                userLogin.getAddress(),
                userLogin.getMobile()
        );
    }

    public void update(User userLogin) {
        userLogin.setFullName(fullName);
        userLogin.setDateOfBirth(dateOfBirth);
        userLogin.setAddress(address);
        userLogin.setMobile(mobile);
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getMobile() {
        return mobile;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/blog/BlogDTO.java'),
    content: `package com.job_web.dto.blog;

import com.job_web.models.Blog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogDTO(
        @NotBlank(message = "{validation.blog.title.required}")
        @Size(max = 255, message = "{validation.blog.title.maxSize}")
        String title,

        @NotBlank(message = "{validation.blog.description.required}")
        String description,

        @NotBlank(message = "{validation.blog.content.required}")
        String content
) {
    public Blog toBlog() {
        Blog blog = new Blog();
        applyTo(blog);
        return blog;
    }

    public void applyTo(Blog blog) {
        blog.setTitle(title);
        blog.setDescription(description);
        blog.setContent(content);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }
}
`
  },
  {
    path: path.join(__dirname, 'src/main/java/com/job_web/dto/blog/LikeDTO.java'),
    content: `package com.job_web.dto.blog;

import jakarta.validation.constraints.NotNull;

public record LikeDTO(
        @NotNull(message = "{validation.blog.id.required}")
        long id
) {
    public long getId() {
        return id;
    }
}
`
  }
];

let success = 0;
let failed = 0;

files.forEach((file, index) => {
  try {
    // Delete if exists
    if (fs.existsSync(file.path)) {
      fs.unlinkSync(file.path);
      console.log(`[${index + 1}] Deleted: ${file.path}`);
    }
    // Write new content
    fs.writeFileSync(file.path, file.content, 'utf-8');
    console.log(`[${index + 1}] Created: ${file.path}`);
    success++;
  } catch (err) {
    console.error(`[${index + 1}] ERROR: ${file.path}`);
    console.error(`    ${err.message}`);
    failed++;
  }
});

console.log(`\n✓ Successfully processed: ${success}/${files.length} files`);
if (failed > 0) console.log(`✗ Failed: ${failed} files`);
