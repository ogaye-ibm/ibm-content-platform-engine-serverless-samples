/*
 * Licensed Materials - Property of IBM (c) Copyright IBM Corp. 2019 All Rights Reserved.
 * 
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 * 
 * DISCLAIMER OF WARRANTIES :
 * 
 * Permission is granted to copy and modify this Sample code, and to distribute modified versions provided that both the
 * copyright notice, and this permission notice and warranty disclaimer appear in all copies and modified versions.
 * 
 * THIS SAMPLE CODE IS LICENSED TO YOU AS-IS. IBM AND ITS SUPPLIERS AND LICENSORS DISCLAIM ALL WARRANTIES, EITHER
 * EXPRESS OR IMPLIED, IN SUCH SAMPLE CODE, INCLUDING THE WARRANTY OF NON-INFRINGEMENT AND THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL IBM OR ITS LICENSORS OR SUPPLIERS BE LIABLE FOR
 * ANY DAMAGES ARISING OUT OF THE USE OF OR INABILITY TO USE THE SAMPLE CODE, DISTRIBUTION OF THE SAMPLE CODE, OR
 * COMBINATION OF THE SAMPLE CODE WITH ANY OTHER CODE. IN NO EVENT SHALL IBM OR ITS LICENSORS AND SUPPLIERS BE LIABLE
 * FOR ANY LOST REVENUE, LOST PROFITS OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, EVEN IF IBM OR ITS LICENSORS OR SUPPLIERS HAVE
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.ibm.ecm.sample.webhook.util;

//import okhttp3.*;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Base64;

/**
 * Utility class for handling calls to the Content Services GraphQL API.
 * 
 * This class will use the TLS 1.2 protocol for calls to the GraphQL API. If a
 * different protocol is required, the protocol can be changed in the code for
 * this class.
 */
public class GraphQLAPIUtil {

    /**
     * Handles calls to the Content Services GraphQL API.
     * 
     * @param graphQLCommand
     *            GraphQL command, either a query or mutation, to pass to the
     *            Content Services GraphQL API
     * @return Response for the call to the Content Services GraphQL API
     */
    public static JSONObject callGraphQLAPI(String graphQLCommand) {
        String method = "callGraphQLAPI";
        WebhookReceiverLogger.entering("GraphQLAPIUtil", method);

        JSONObject jsonGraphQLResponse = null;
        org.apache.http.HttpResponse response = null;
        //okhttp3.Response response = null;
        CloseableHttpClient httpClient = null;
        BufferedReader breader = null;

        try {
            String csServerUsername = CSServerInfo.CS_SERVER_USERNAME;
            String csServerPassword = CSServerInfo.CS_SERVER_PASSWORD;
            String csServerURL = CSServerInfo.CS_SERVER_GRAPHQL_URL;
            
            /*
             * Create HTTPClient and have it use TLSv1.2. If you do not wish to
             * force TLSv1.2 to be used, you can change the protocol or
             * completely remove the setSSLSocketFactory() call from the
             * HttpClientBuilder call.
             */
            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(
                    SSLContexts.createDefault(), new String[] { "TLSv1.2" },
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            httpClient = HttpClientBuilder.create().setSSLSocketFactory(sslConnectionSocketFactory).build();
            //httpClient = HttpClientBuilder.create().build();


            /*
             * The sample application uses basic authentication when connecting
             * to the GraphQL API. In a real environment, it would probably be
             * better to use OIDC (such as via the UMS container) to handle
             * authentication between the Webhook Receiver application and
             * GraphQL.
             */
            String credentials = csServerUsername + ":" + csServerPassword;
            String encoding = Base64.getEncoder().encodeToString(
                    credentials.getBytes("UTF-8"));

            /*
             * Build the URL to the GraphQL API using the base URL from
             * CSServerInfo.properties and adding the path for GraphQL
             */
            URI uri = new URIBuilder(csServerURL).build();

            // Set headers for GraphQL API call
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            httpPost.addHeader("content-type", "application/json");
            httpPost.addHeader("Accept", "application/json");

            /*
             * Pass GraphQL API call via the value for a query parameter using
             * JSON.
             * 
             * Note that while GraphQL itself is similar to JSON, it is
             * different from JSON in subtle ways and thus has to be passed as a
             * String, rather than converted to a JSON object.
             */
            JSONObject jsonGraphQLCommand = new JSONObject();
            jsonGraphQLCommand.put("query", graphQLCommand);
            StringEntity jsonQueryEntity = new StringEntity(
                    jsonGraphQLCommand.toString());
            httpPost.setEntity(jsonQueryEntity);

            // Trace statement to show the arguments for the GraphQL API call
            WebhookReceiverLogger.info("calling GraphQLAPI on " + csServerURL
                    + "  with " + graphQLCommand);
            WebhookReceiverLogger.info("httpPost = " + httpPost);

            // Handle the response
            try {
                /*OkHttpClient client = new OkHttpClient().newBuilder().build();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, "{\"query\":\"query domainquery {\\n  domain {\\n    id\\n    name\\n    properties {\\n      id\\n    }\\n  }\\n}\",\"variables\":{}}");
                Request request = new Request.Builder()
                        .url("https://cpd-cp4ba.itzroks-550004cpmg-j51i84-4b4a324f027aea19c5cbc0c3275c4656-0000.us-east.containers.appdomain.cloud/content-services-graphql/graphql?query=query domainquery {domain {id name properties { id } } }")
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Basic Y3A0YWRtaW46Wm05Y3ZmbFY1ZGRIb0V4bXFtQXI=")
                        .build();
                response = client.newCall(request).execute();
                System.out.println("******* RESPONSE IS: " + response.body().string());*/

                response = httpClient.execute(httpPost);
            }catch (Exception e){
                e.printStackTrace();
            }
            StatusLine statusLine = response.getStatusLine();

            // Trace statement to show the response from the GraphQL API call
            WebhookReceiverLogger.info("statusLine = " + statusLine.toString());
            WebhookReceiverLogger.info("response = " + response);

            // Get response string
            breader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder responseString = new StringBuilder();
            String line = "";
            while ((line = breader.readLine()) != null) {
                responseString.append(line);
            }

            /*
             * Parse response string as a JSON object and use a trace statement
             * to show the formatted response
             */
            String responseGraphQL = responseString.toString();
            jsonGraphQLResponse = new JSONObject(responseGraphQL);
            WebhookReceiverLogger.info("responseGraphQL = "
                    + jsonGraphQLResponse.toString(2) + "\n\n");

        } catch (Exception ex) {
            WebhookReceiverLogger.error(
                    "Could not submit GraphQL API call.", ex);
        } finally {
            // Close the Buffered Reader
            try {
                if (breader != null) {
                    breader.close();
                }
            } catch (IOException e) {

            }
            // Close the HTTP connection
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {

            }
            
            WebhookReceiverLogger.exiting("GraphQLAPIUtil", method);
        }

        return jsonGraphQLResponse;
    }
}
