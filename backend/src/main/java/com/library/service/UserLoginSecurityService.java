package com.library.service;

import com.library.model.LoginLog;
import com.library.model.User;
import com.library.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserLoginSecurityService {

    private static final Logger log = LoggerFactory.getLogger(UserLoginSecurityService.class);

    private final LoginLogRepository loginLogRepository;
    private final Clock clock;
    private final Map<String, Integer> loginFailures = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockedAccounts = new ConcurrentHashMap<>();

    public UserLoginSecurityService(LoginLogRepository loginLogRepository, Clock clock) {
        this.loginLogRepository = loginLogRepository;
        this.clock = clock;
    }

    public void assertNotLocked(String studentId, String ipAddress, String userAgent) {
        if (!isAccountLocked(studentId)) {
            return;
        }

        recordLoginLog(null, studentId, ipAddress, userAgent, 0, "账号已被临时锁定");
        throw new IllegalArgumentException("账号已被临时锁定，请 30 分钟后再试。");
    }

    public void handleLoginFailure(String studentId, String ipAddress, String userAgent, String reason) {
        int failures;
        boolean shouldLock = false;

        synchronized (loginFailures) {
            failures = loginFailures.compute(studentId, (key, value) -> (value == null ? 0 : value) + 1);
            if (failures >= 5 && !lockedAccounts.containsKey(studentId)) {
                lockedAccounts.put(studentId, now());
                shouldLock = true;
            }
        }

        if (shouldLock) {
            log.warn("账号 {} 连续登录失败 {} 次，已被临时锁定", studentId, failures);
            recordLoginLog(null, studentId, ipAddress, userAgent, 0, "登录失败次数过多，账号已临时锁定");
            return;
        }

        recordLoginLog(null, studentId, ipAddress, userAgent, 0, reason + "，累计失败 " + failures + " 次");
    }

    public void recordDisabledAccountAttempt(User user, String ipAddress, String userAgent) {
        recordLoginLog(user.getId(), user.getStudentId(), ipAddress, userAgent, 0, "账号已停用");
    }

    public void clearFailures(String studentId) {
        loginFailures.remove(studentId);
    }

    public void recordLoginSuccess(User user, String ipAddress, String userAgent) {
        recordLoginLog(user.getId(), user.getStudentId(), ipAddress, userAgent, 1, null);
    }

    @Transactional(readOnly = true)
    public List<LoginLog> getLoginLogs(String studentId, int limit) {
        return loginLogRepository.findByStudentIdOrderByLoginTimeDescIdDesc(
            studentId,
            PageRequest.of(0, Math.max(limit, 1))
        );
    }

    public void clearStateForTests() {
        loginFailures.clear();
        lockedAccounts.clear();
    }

    private boolean isAccountLocked(String studentId) {
        LocalDateTime lockTime = lockedAccounts.get(studentId);
        if (lockTime == null) {
            return false;
        }

        if (now().isAfter(lockTime.plusMinutes(30))) {
            lockedAccounts.remove(studentId);
            loginFailures.remove(studentId);
            return false;
        }

        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void recordLoginLog(Long userId, String studentId, String ipAddress,
                                  String userAgent, Integer status, String failReason) {
        loginLogRepository.save(new LoginLog(userId, studentId, ipAddress, userAgent, status, failReason));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
