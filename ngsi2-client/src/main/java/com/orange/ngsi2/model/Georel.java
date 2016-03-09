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

package com.orange.ngsi2.model;

import java.util.Optional;

/**
 * Georel model
 */
public class Georel {

    GeorelEnum georel;

    Optional<ModifierEnum> modifier;

    Optional<Float> distance = null;

    public Georel(GeorelEnum georel) {
        this.georel = georel;
    }

    public Georel(GeorelEnum georel, Optional<ModifierEnum> modifier, Optional<Float> distance) {
        this.georel = georel;
        this.modifier = modifier;
        this.distance = distance;
    }

    public GeorelEnum getGeorel() {
        return georel;
    }

    public void setGeorel(GeorelEnum georel) {
        this.georel = georel;
    }

    public Optional<ModifierEnum> getModifier() {
        return modifier;
    }

    public void setModifier(Optional<ModifierEnum> modifier) {
        this.modifier = modifier;
    }

    public Optional<Float> getDistance() {
        return distance;
    }

    public void setDistance(Optional<Float> distance) {
        this.distance = distance;
    }
}
