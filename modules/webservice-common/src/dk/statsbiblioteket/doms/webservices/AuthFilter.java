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
                Credentials creds = ExtractCredentials.extract(httpServletRequest);
                request.setAttribute("Credentials",creds);
                chain.doFilter(request,response);
            } catch (CredentialsException e) {        //else send request for authenticate
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                    httpServletResponse.addHeader("WWW-Authenticate","BASIC "+realm);
                    httpServletResponse.sendError(401);
                }
            }
        }
    }

    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
