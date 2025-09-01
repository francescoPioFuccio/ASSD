package com.example.app1.ui.login;


 public class LoggedInUserView {
 private String displayName;
 private String userId;
 private String preferenzeSync;

 public LoggedInUserView(String displayName, String userId) {
 this.displayName = displayName;
 this.userId = userId;
 }

 public LoggedInUserView(String displayName, String userId, String preferenzeSync) {
 this.displayName = displayName;
 this.userId = userId;
 this.preferenzeSync = preferenzeSync;
 }

 public String getDisplayName() {
 return displayName;
 }

 public String getUserId() {
 return userId;
 }

 public String getPreferenzeSync() {
 return preferenzeSync;
 }
 }