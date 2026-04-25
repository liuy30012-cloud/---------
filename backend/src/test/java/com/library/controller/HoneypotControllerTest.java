package com.library.controller;

import com.library.service.IpBanService;
import com.library.service.SecurityMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoneypotControllerTest {

    @Test
    void dryRunDoesNotBanClient() {
        IpBanService ipBanService = mock(IpBanService.class);
        SecurityMetricsService securityMetricsService = mock(SecurityMetricsService.class);
        HoneypotController controller = new HoneypotController(ipBanService, securityMetricsService);
        ReflectionTestUtils.setField(controller, "honeypotEnabled", true);
        ReflectionTestUtils.setField(controller, "honeypotDryRun", true);
        ReflectionTestUtils.setField(controller, "banDuration", 300);
        when(ipBanService.getViolationCount("203.0.113.7")).thenReturn(2L);
        when(ipBanService.getTrackedViolationCount()).thenReturn(5);
        when(ipBanService.getBannedIpCount()).thenReturn(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("User-Agent", "Mozilla/5.0");

        controller.fakeGraphql(request);

        verify(ipBanService, never()).banIp("203.0.113.7", 300);
    }
}
