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

import com.orange.ngsi2.exception.UnsupportedOperationException;
import com.orange.ngsi2.model.Attribute;
import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.utility.Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/v2/i")
public class FakeNgsi2ControllerHelper extends Ngsi2BaseController {

    @Override
    protected Map<String, String> getResources() throws Exception {
        return Utils.createListResourcesReference();
    }

    @Override
    protected List<Entity> getEntities(Optional<String> id, Optional<String> type, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
        if (id.isPresent() && id.get().equals("Bcn-Welt")) {
            return Collections.singletonList(Utils.createEntityBcnWelt());
        }
        return Utils.createListEntitiesConflictingReference();
    }

    @Override
    protected String createEntity(Entity entity){
        return "Bcn-Welt";
    }

    @Override
    protected void updateEntity(String entityId, HashMap<String, Attribute> attributes){ }

    @Override
    protected void updateExistingAttributes(String entityId, HashMap<String, Attribute> attributes){ }

    @Override
    protected void replaceAllExistingAttributes(String entityId, HashMap<String, Attribute> attributes){ }

    @Override
    protected void removeEntity(String entityId){ }

}
