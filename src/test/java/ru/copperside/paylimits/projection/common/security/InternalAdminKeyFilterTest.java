package ru.copperside.paylimits.projection.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalAdminKeyFilterTest {

    @Test
    void rejectsMissingKeyWhenRequired() throws Exception {
        InternalAdminKeyFilter filter = new InternalAdminKeyFilter(new InternalAdminKeyProperties("secret", true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/limit-projection/reservations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsValidKeyWhenRequired() throws Exception {
        InternalAdminKeyFilter filter = new InternalAdminKeyFilter(new InternalAdminKeyProperties("secret", true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/limit-projection/reservations");
        request.addHeader("X-Internal-Admin-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsNonInternalPaths() throws Exception {
        InternalAdminKeyFilter filter = new InternalAdminKeyFilter(new InternalAdminKeyProperties("secret", true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
