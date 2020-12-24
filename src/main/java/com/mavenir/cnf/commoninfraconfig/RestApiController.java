// Copyright 2020 Mavenir
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.xgvela.cnf.commoninfraconfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tmaas")
public class RestApiController {

	@Autowired
	RestApiHelper restApiHelper;
	private static Logger LOG = LogManager.getLogger(RestApiController.class);

	@GetMapping("/config/common-infra")
	public @ResponseBody ResponseEntity<String> getStaticConfig() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		LOG.info("Recieved request for common-infra");
		String response = restApiHelper.getStaticConfig();
		if (!response.contains("try again"))
			return new ResponseEntity<String>(response, headers, HttpStatus.OK);
		else
			return new ResponseEntity<String>(response, headers, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
