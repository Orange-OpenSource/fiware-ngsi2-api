/*
 * Copyright (C) 2016 Orange
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
 *
 */

package com.orange.ngsi2.server;

import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.utility.Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v2/i")
public class FakeNgsi2ControllerHelper extends Ngsi2BaseController {

    @Override
    protected Map<String, String> getResources() throws Exception {
        return Utils.createListResourcesReference();
    }

    @Override
    protected List<Entity> getEntities(Optional<String> id, Optional<String> type, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
        return Utils.createListEntitiesReference();
    }

    @Override
    protected String createEntity(Entity entity){
        return "Bcn-Welt";
    }

}
