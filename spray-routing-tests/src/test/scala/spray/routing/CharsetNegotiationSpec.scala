/*
 * Copyright (C) 2011-2012 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.routing

import spray.http._
import HttpHeaders._
import MediaTypes._
import HttpCharsets._


class CharsetNegotiationSpec extends RoutingSpec {

  val Hällo = complete("Hällö")

  "The framework" should {
    "encode text content using ISO-8859-1 if no Accept-Charset header is present in the request" in {
      Get() ~> Hällo ~> check {
        body === HttpBody(ContentType(`text/plain`, `ISO-8859-1`), "Hällö")
      }
    }
    "encode text content using ISO-8859-1 if the Accept-Charset header contains '*'" in {
      Get() ~> `Accept-Charset`(`UTF-8`, `*`) ~> Hällo ~> check {
        contentType === ContentType(`text/plain`, `ISO-8859-1`)
        entityAs[String] === "Hällö"
      }
    }
    "encode text content using the first charset in the Accept-Charset header if '*' is not present" in {
      Get() ~> `Accept-Charset`(`UTF-8`) ~> Hällo ~> check {
        contentType === ContentType(`text/plain`, `UTF-8`)
        entityAs[String] === "Hällö"
      }
    }
  }

}