// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.List;
=======
>>>>>>> 357f4aab84af1746bbe36443f4a54f454ab7e4e1

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  static final String CONTENT_TYPE = "application/json;";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List messages = new ArrayList<String>();
    messages.add("Nice portfolio!");
    messages.add("I found the fun stuff ;)");
    messages.add("Bark!");
    Gson gson = new Gson();
    String json = gson.toJson(messages);
    response.setContentType(CONTENT_TYPE);
    response.getWriter().println(json);
  }
}
