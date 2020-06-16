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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * Servlet that returns some example content. TODO: modify this file to handle
 * comments data
 */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private static final String COMMENT = "Comment";
  private static final String CONTENT = "content";
  private static final String CONTENT_TYPE = "application/json";
  private static final String IMAGE = "image";
  private static final String LANGUAGE = "language";
  private static final String MAX_COMMENTS = "max";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String maxContent = request.getParameter(MAX_COMMENTS);
    int maxComments;
    try {
      maxComments = Integer.parseInt(maxContent);
    } catch (NumberFormatException e) {
      System.err.println("Could not convert to int: " + maxContent);
      response.sendError(400, "Invalid parameter \"max\" in request.");
      return;
    }
    String language = request.getParameter(LANGUAGE);
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Comment> comments = new ArrayList<>();
    List<String> messages = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      if (messages.size() >= maxComments) {
        break;
      }
      String content = (String) entity.getProperty(CONTENT);
      String imageURL = (String) entity.getProperty(IMAGE);
      // messages is used to translate all comments in one call
      messages.add(content);
      // comments pairs each comment with its associated image to be exported as
      // JSON
      comments.add(new Comment(content, imageURL));
    }
    try {
      if (comments.size() > 0) {
        Translate translateService = TranslateOptions.getDefaultInstance().getService();
        List<Translation> translations = translateService.translate(messages,
            Translate.TranslateOption.targetLanguage(language));
        for (int i = 0; i < comments.size(); i++) {
          comments.get(i).content = translations.get(i).getTranslatedText();
        }
      } else {
        comments.add(new Comment("No comments yet."));
      }
      Gson gson = new Gson();
      String json = gson.toJson(comments);
      response.setContentType(CONTENT_TYPE);
      response.getWriter().println(json);
    } catch (TranslateException e) {
      System.err.println(e.getMessage());
      response.sendError(400, "Invalid translation request.");
      return;
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String content = request.getParameter(CONTENT).trim();
    long timestamp = System.currentTimeMillis();
    if (content.isEmpty()) {
      System.err.println("Empty comment submitted");
      response.sendError(400, "Empty comment submitted");
      return;
    }
    Entity commentEntity = new Entity(COMMENT);
    commentEntity.setProperty("content", content);
    commentEntity.setProperty("timestamp", timestamp);
    String imageURL = getUploadedFileUrl(request);
    if (imageURL != null) {
      commentEntity.setProperty("image", imageURL);
    }
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    response.sendRedirect("/#comments");
  }

  /**
   * Returns a URL that points to the uploaded file, or null if the user didn't
   * upload a file.
   */
  private String getUploadedFileUrl(HttpServletRequest request) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(IMAGE);

    // User submitted form without selecting a file, so we can't get a URL. (dev
    // server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live
    // server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // We could check the validity of the file here, e.g. to make sure it's an image
    // file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must
    // use the relative
    // path to the image, rather than the path returned by imagesService which
    // contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    }
  }
}
