package com.ulms.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ulms.config.CustomPasswordEncoder;
import com.ulms.exception.ApiException;
import com.ulms.security.OtpGenerator;
import com.ulms.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthValidate authValidate = new AuthValidate();
    private final UserRepository userRepository;
    private final CustomPasswordEncoder customPasswordEncoder;
    private final UserTokenRepository userTokenRepository;
    private final UserAuthRepository userAuthRepository;
    private final EmailService emailService;
    private final com.ulms.security.JwtService jwtService;

    @Value("${google.client-id}")
    private String googleClientId;

    //================================
    //REGISTER LOCAL ACCOUNT
    @Override
    public void register(RegisterRequest registerRequest) {
        String fullName = registerRequest.getFullName().trim().replaceAll("\\s+", " ");
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();
        String phone = registerRequest.getPhone();
        String address = registerRequest.getAddress();

        if (!authValidate.validateFullName(fullName)) {
            throw new ApiException("Họ và tên không hợp lệ. Vui lòng chỉ sử dụng chữ cái, độ dài từ 2-100 ký tự và không chứa số.");
        }

        if (!authValidate.validateEmail(email)) {
            throw new ApiException("Email không hợp lệ. Vui lòng kiểm tra lại định dạng (ví dụ: name@example.com).");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email đã được sử dụng, hãy thử đăng nhập bằng Google Account");
        }

        if (!authValidate.validatePhoneNumber(phone)) {
            throw new ApiException("Số điện thoại không hợp lệ. Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0.");
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("Số điện thoại này đã được sủ dụng");
        }

        if (!authValidate.validatePassword(password)) {
            throw new ApiException("Mật khẩu không hợp lệ. Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt (@, $, !, %, , ?, &).");
        }

        if (address == null) {
            throw new ApiException("Địa chỉ không hợp lệ");
        }

        //Lưu lại tài khoản mới tạo
        Role defaultRole = new Role();
        defaultRole.setRoleId((byte) 1); // 1: READER

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPasswordHash(customPasswordEncoder.encode(password));
        newUser.setPhone(phone);
        newUser.setAddress(address);
        newUser.setRole(defaultRole);
        newUser.setStatus((byte) 0); // 0: PENDING_VERIFICATION
        newUser.setCreatedAt(LocalDateTime.now());

        System.out.println(newUser);
        User savedUser = userRepository.save(newUser);

        //Tạo OTP để người dùng xác thực tài khoản
        Long userId = savedUser.getUserId();
        System.out.println("User ID vừa tạo là: " + userId);

        //Gửi email
        generateOTP(userId, email, false);
    }

    @Override
    public void resendEmailVerification(EmailVerifyRequest request) {
        //Kiểm tra tài khoản đã được tạo chưa
        System.out.println("Email cần tìm: " + request.getEmail());
        User user = findUserByEmail(request.getEmail());
        if (user == null) {
            throw new ApiException("Email không tồn tại trong hệ thống");
        }
        // Kiểm tra loại OTP cần gửi
        // tokenType = false (0): Xác thực tài khoản
        // tokenType = true  (1): Reset password
        if (!request.getTokenType()){
            // Nếu xác thực tài khoản thì User phải ở trạng thái chưa xác thực
            if(user.getStatus() != 0) {
                throw new ApiException("Tài khoản đã xác thực");
            }
        } else {
            // Nếu reset password thì User phải ở trạng thái đã xác thực
            if (user.getStatus() !=1) {
                throw new ApiException("Tài khoản hiện đang bị khoá hoặc chưa thực hiện xác thực");
            }
        }
        //Gửi email
        generateOTP(user.getUserId(), user.getEmail(),request.getTokenType());
    }

    @Override
    public void verifyEmail(EmailVerifyRequest request) {
        String email = request.getEmail();
        String OTP = request.getOTP();
        boolean tokenType = request.getTokenType();

        User user = findUserByEmail(email);
        if (user == null) {
            throw new ApiException("Email không tồn tại trong hệ thống");
        }

        if (user.getStatus() != 0 ){
            throw new ApiException("Tài khoản đã xác thực");
        }

        if (tokenType){
            throw new ApiException("Loại OTP không hợp lệ");
        }

        UserToken userToken = userTokenRepository.findByUser_UserIdAndTokenType(user.getUserId(), tokenType).orElse(null);
        if (userToken == null) {
            throw new ApiException("Không tìm thấy OTP. Vui lòng yêu cầu gửi OTP mới");
        }
        System.out.println("OTP hash DB: "+ userToken.getTokenHash());

        if (!customPasswordEncoder.matches(OTP,userToken.getTokenHash())){
            throw new ApiException("OTP không chính xác. Vui lòng nhập lại");
        }
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new ApiException("OTP hết hạn sử dụng. Vui lòng nhận OTP mới");
        }
        if (userToken.getUsed()){
            throw new ApiException("OTP đã được sử dụng. Vui lòng nhận OTP mới");
        }

        user.setStatus((byte) 1);
        userRepository.save(user);

        userToken.setUsed(true);
        userTokenRepository.save(userToken);
    }

    @Override
    public void resetPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        String OTP = request.getOTP();
        String newPassword = request.getNewPassword();
        boolean tokenType = request.getTokenType();

        User user = findUserByEmail(email);
        if (user == null) {
            throw new ApiException("Email không tồn tại trong hệ thống");
        }

        if (user.getStatus() != 1 ){
            throw new ApiException("Tài khoản chưa được xác thực hoặc không ở trạng thái hoạt động");
        }

        if (!tokenType){
            throw new ApiException("Loại OTP không hợp lệ");
        }

        UserToken userToken = userTokenRepository.findByUser_UserIdAndTokenType(user.getUserId(), tokenType).orElse(null);
        if (userToken == null) {
            throw new ApiException("Không tìm thấy OTP. Vui lòng yêu cầu gửi OTP mới");
        }
        System.out.println("OTP hash DB: "+ userToken.getTokenHash());

        if (!customPasswordEncoder.matches(OTP,userToken.getTokenHash())){
            throw new ApiException("OTP không chính xác. Vui lòng nhập lại");
        }
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new ApiException("OTP hết hạn sử dụng. Vui lòng nhận OTP mới");
        }
        if (userToken.getUsed()){
            throw new ApiException("OTP đã được sử dụng. Vui lòng nhận OTP mới");
        }

        if (!authValidate.validatePassword(newPassword)) {
            throw new ApiException("Mật khẩu không hợp lệ. Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt (@, $, !, %, , ?, &).");
        }

        user.setPasswordHash(customPasswordEncoder.encode(newPassword));
        userRepository.save(user);

        userToken.setUsed(true);
        userTokenRepository.save(userToken);

    }

    public User findUserByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public void generateOTP(Long userId, String email, Boolean tokenType) {
        // Tạo OTP
        String otp = OtpGenerator.generateOtp();
        System.out.println("OTP xác thực tài khoản: " + otp);
        // Tìm token cũ
        UserToken userToken = userTokenRepository.findByUser_UserIdAndTokenType(userId, tokenType).orElse(new UserToken());

        // Cập nhật token
        userToken.setUser(userRepository.getReferenceById(userId));
        userToken.setTokenType(tokenType);
        userToken.setTokenHash(customPasswordEncoder.encode(otp));
        userToken.setCreatedAt(LocalDateTime.now());
        userToken.setUsed(false);
        userToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        userTokenRepository.save(userToken);

        // Gửi OTP
        emailService.sendOtpEmail(email, otp);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new ApiException("Vui lòng nhập đầy đủ email và mật khẩu");
        }

        String email = request.getEmail().trim();
        String password = request.getPassword();

        if (email.isEmpty() || password.isEmpty()) {
            throw new ApiException("Email và mật khẩu không được để trống");
        }

        User user = findUserByEmail(email);
        if (user == null) {
            throw new ApiException("Email hoặc mật khẩu không chính xác");
        }

        // Kiểm tra trạng thái tài khoản
        // 0: PENDING_VERIFICATION, 1: ACTIVE, 2: LOCKED, 3: INACTIVE
        if (user.getStatus() == 0) {
            throw new ApiException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước khi đăng nhập");
        } else if (user.getStatus() == 2) {
            throw new ApiException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên");
        } else if (user.getStatus() == 3) {
            throw new ApiException("Tài khoản hiện đang ngưng hoạt động");
        }

        // So khớp mật khẩu
        if (!customPasswordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("Email hoặc mật khẩu không chính xác");
        }

        // Cập nhật thời gian đăng nhập lần cuối
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Sinh JWT token
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "READER";
        String accessToken = jwtService.generateToken(user.getUserId(), user.getEmail(), roleName);

        // Đóng gói response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setAddress(user.getAddress());
        response.setRole(roleName);
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setStatus(user.getStatus());

        return response;
    }

    @Override
    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().trim().isEmpty()) {
            throw new ApiException("Vui lòng cung cấp Google idToken");
        }

        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken().trim());
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        if (email == null || email.trim().isEmpty()) {
            throw new ApiException("Không tìm thấy email từ tài khoản Google");
        }

        User user;
        // 1. Kiểm tra xem tài khoản Google này đã từng liên kết chưa
        Optional<UserAuth> userAuthOpt = userAuthRepository.findByProviderAndProviderId("GOOGLE", googleSub);
        if (userAuthOpt.isPresent()) {
            user = userAuthOpt.get().getUser();
        } else {
            // 2. Chưa có trong user_auth, kiểm tra email đã tồn tại trong users chưa
            user = findUserByEmail(email);
            if (user == null) {
                // Tạo mới User
                Role defaultRole = new Role();
                defaultRole.setRoleId((byte) 1); // 1: READER

                user = new User();
                user.setFullName(name != null ? name : email.split("@")[0]);
                user.setEmail(email);
                user.setPasswordHash(null); // Google Login không có password cục bộ
                user.setAvatarUrl(pictureUrl);
                user.setRole(defaultRole);
                user.setStatus((byte) 1); // Đã xác thực qua Google
                user.setCreatedAt(LocalDateTime.now());
                user = userRepository.save(user);
            } else {
                // Nếu User đã tồn tại từ trước (đăng ký thường), cập nhật avatar nếu chưa có
                if (user.getAvatarUrl() == null && pictureUrl != null) {
                    user.setAvatarUrl(pictureUrl);
                }
                // Nếu tài khoản trước đó ở status 0 (chưa verify), Google đã verify email rồi nên kích hoạt luôn
                if (user.getStatus() == 0) {
                    user.setStatus((byte) 1);
                }
                user = userRepository.save(user);
            }

            // Tạo liên kết trong user_auth
            UserAuth newAuth = new UserAuth();
            newAuth.setUser(user);
            newAuth.setProvider("GOOGLE");
            newAuth.setProviderId(googleSub);
            newAuth.setProviderEmail(email);
            newAuth.setCreatedAt(LocalDateTime.now());
            userAuthRepository.save(newAuth);
        }

        // Kiểm tra tài khoản có bị khóa không
        if (user.getStatus() == 2) {
            throw new ApiException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên");
        } else if (user.getStatus() == 3) {
            throw new ApiException("Tài khoản hiện đang ngưng hoạt động");
        }

        // Cập nhật last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Sinh JWT Access Token của hệ thống
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "READER";
        String accessToken = jwtService.generateToken(user.getUserId(), user.getEmail(), roleName);

        // Trả về LoginResponse
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setAddress(user.getAddress());
        response.setRole(roleName);
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setStatus(user.getStatus());

        return response;
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ApiException("Google ID Token không hợp lệ hoặc đã hết hạn");
            }
            return idToken.getPayload();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Xác thực Google Token thất bại: " + e.getMessage());
        }
    }
}

