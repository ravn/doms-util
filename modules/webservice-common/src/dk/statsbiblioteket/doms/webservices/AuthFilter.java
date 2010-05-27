package dk.statsbiblioteket.doms.webservices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import java.io.IOException;

public class AuthFilter implements Filter {

    private FilterConfig filterConfig;

    private String realm;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        realm = filterConfig.getInitParameter("Realm name");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //if Authenticate header already set
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            try {
                if (!"wsdl".equals(httpServletRequest.getQueryString())) {
                    Credentials creds = ExtractCredentials.extract(httpServletRequest);
                    request.setAttribute("Credentials",creds);
                }
            } catch (CredentialsException e) {        //else send request for authenticate
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                    httpServletResponse.addHeader("WWW-Authenticate","BASIC "+realm);
                    httpServletResponse.sendError(401);
                    return;
                }
            }
        }
        chain.doFilter(request,response);
    }

    public void destroy() {
    }
}
