/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.write;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerClientTest {

  MockHttpServer server = null;
  BootstrapSettings settings;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Before
  public void before(){
    settings = mock(BootstrapSettings.class);
  }

  @Test
  public void shouldRemoveUrlEndingSlash() throws Exception {
    BootstrapSettings settings = mock(BootstrapSettings.class);
    when(settings.getProperty(eq("sonar.host.url"), anyString())).thenReturn("http://localhost:8080/sonar/");

    ServerClient client = new ServerClient(settings, new EnvironmentInformation("Junit", "4"));

    assertThat(client.getURL()).isEqualTo("http://localhost:8080/sonar");
  }

  @Test
  public void should_request_url() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseData("this is the content");

    assertThat(newServerClient().request("/foo")).isEqualTo("this is the content");
  }

  @Test
  public void should_download_file() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseData("this is the content");

    File file = temp.newFile();
    newServerClient().download("/foo", file);
    assertThat(Files.toString(file, Charsets.UTF_8)).isEqualTo("this is the content");
  }

  @Test
  public void should_fail_if_unauthorized_with_login_password_not_provided() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(401);

    thrown.expectMessage("Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");
    newServerClient().request("/foo");
  }

  @Test
  public void should_fail_if_unauthorized_with_login_password_provided() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(401);

    when(settings.getProperty(eq("sonar.login"))).thenReturn("login");
    when(settings.getProperty(eq("sonar.password"))).thenReturn("password");

    thrown.expectMessage("Not authorized. Please check the properties sonar.login and sonar.password");
    newServerClient().request("/foo");
  }

  @Test
  public void should_fail_if_error() throws Exception {
    server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(500);

    thrown.expectMessage("Fail to execute request [code=500, url=http://localhost:" + server.getPort() + "/foo]");
    newServerClient().request("/foo");
  }

  private ServerClient newServerClient() {
    when(settings.getProperty(eq("sonar.host.url"), anyString())).thenReturn("http://localhost:" + server.getPort());
    return new ServerClient(settings, new EnvironmentInformation("Junit", "4"));
  }

  static class MockHttpServer {
    private Server server;
    private String responseBody;
    private String requestBody;
    private String mockResponseData;
    private int mockResponseStatus = SC_OK;

    public void start() throws Exception {
      server = new Server(0);
      server.setHandler(getMockHandler());
      server.start();
    }

    /**
     * Creates an {@link org.mortbay.jetty.handler.AbstractHandler handler} returning an arbitrary String as a response.
     *
     * @return never <code>null</code>.
     */
    public Handler getMockHandler() {
      Handler handler = new AbstractHandler() {

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
          Request baseRequest = request instanceof Request ? (Request) request : HttpConnection.getCurrentConnection().getRequest();
          setResponseBody(getMockResponseData());
          setRequestBody(IOUtils.toString(baseRequest.getInputStream()));
          response.setStatus(mockResponseStatus);
          response.setContentType("text/xml;charset=utf-8");
          write(getResponseBody(), response.getOutputStream());
          baseRequest.setHandled(true);
        }
      };
      return handler;
    }

    public void stop() {
      try {
        if (server != null) {
          server.stop();
        }
      } catch (Exception e) {
        throw new IllegalStateException("Fail to stop HTTP server", e);
      }
    }

    public void setResponseBody(String responseBody) {
      this.responseBody = responseBody;
    }

    public String getResponseBody() {
      return responseBody;
    }

    public void setRequestBody(String requestBody) {
      this.requestBody = requestBody;
    }

    public String getRequestBody() {
      return requestBody;
    }

    public void setMockResponseData(String mockResponseData) {
      this.mockResponseData = mockResponseData;
    }

    public void setMockResponseStatus(int status) {
      this.mockResponseStatus = status;
    }

    public String getMockResponseData() {
      return mockResponseData;
    }

    public int getPort() {
      return server.getConnectors()[0].getLocalPort();
    }
  }

}
