# RoomMate

CS 175 group project (Group 2): Aryan, Ananya, Prateek.

An Android app for roommates. Find a match, chat, split bills, rotate chores and share a grocery list.

## Tech

- Java, XML layouts
- Min SDK 24, target SDK 34
- Firebase Auth and Firestore

## Run

1. Open the folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or phone.

## Structure

```
app/src/main/java/edu/sjsu/cs175/roommate/
  MainActivity.java        bottom navigation host
  auth/LoginActivity.java  login and sign up
  auth/ProfileActivity.java
  ui/MatchesFragment.java  roommate matching
  ui/ChatFragment.java
  ui/ExpensesFragment.java expense split
  ui/ChoresFragment.java   chores calendar and reminders
  ui/GroceryFragment.java
  ui/LeaseInfoActivity.java
```

Profile, Lease Info and Log out are in the top-right menu.
