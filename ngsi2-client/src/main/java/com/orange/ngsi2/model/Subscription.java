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

import java.time.Instant;
import java.util.Date;

/**
 * Subscription model
 */
public class Subscription {

    String id;

    SubjectSubscription subject;

    Notification notification;

    Instant expires;

    StatusEnum status;

    public Subscription() {
    }

    public Subscription(String id, SubjectSubscription subject, Notification notification, Instant expires, StatusEnum status) {
        this.id = id;
        this.subject = subject;
        this.notification = notification;
        this.expires = expires;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubjectSubscription getSubject() {
        return subject;
    }

    public void setSubject(SubjectSubscription subject) {
        this.subject = subject;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Instant getExpires() {
        return expires;
    }

    public void setExpires(Instant expires) {
        this.expires = expires;
    }
}
