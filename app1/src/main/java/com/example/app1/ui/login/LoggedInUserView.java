package com.example.app1.ui.login;


 public class LoggedInUserView {
 private String displayName;
 private String userId;

 public LoggedInUserView(String displayName, String userId) {
 this.displayName = displayName;
 this.userId = userId;
 }

 public String getDisplayName() {
 return displayName;
 }

 public String getUserId() {
 return userId;
 }
 }