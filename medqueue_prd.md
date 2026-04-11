# MVP Product Requirements Document (PRD)

## 1. Product Name
Medical Shop Request Manager (MVP)

## 2. One-line Summary
A very simple Android app to save customer medicine requests and notify them via WhatsApp when medicines arrive.

## 3. Problem (MVP Focus)
Shop worker forgets or struggles to track who asked for which medicine and wastes time manually messaging customers.

## 4. MVP Goal
Solve ONLY this:
- Save request fast
- See pending requests
- Send WhatsApp message in one tap
- Mark as delivered

Nothing else.

## 5. Target User
- Single medical shop worker
- Uses own Android phone
- Needs speed, not features

## 6. Core Flow (ONLY FLOW)
1. Enter Name + Phone + Medicine
2. Tap Save
3. Open Requests list
4. Tap item
5. Tap “Send Msg”
6. Tap “Delivered”

## 7. Screens (ONLY 3)

### 7.1 Home Screen
Fields:
- Name
- Phone
- Medicine

Button:
- Save

---

### 7.2 Request List
Show:
- Medicine
- Name
- Phone
- Status (Pending / Delivered)

Tap → open detail

---

### 7.3 Request Detail
Show:
- Name
- Phone
- Medicine

Buttons:
- Send Msg
- Delivered

## 8. Core Features (STRICT)

### 8.1 Save Request
- Store name, phone, medicine
- Status = Pending

### 8.2 List Requests
- Show all requests
- Show status

### 8.3 Send WhatsApp Message
Use intent:
- Open WhatsApp
- Prefill message

Message:
Hello {Name}, your {Medicine} is now available. Please visit.

### 8.4 Mark Delivered
- Change status to Delivered

## 9. Data Model (Minimal)

Request:
- id
- name
- phone
- medicine
- status (PENDING / DELIVERED)

## 10. Validation (Basic)
- name not empty
- phone not empty
- medicine not empty

## 11. Non-Functional (Minimal)
- works offline
- fast to open
- simple UI

## 12. Out of Scope (DO NOT BUILD)
- login
- backend
- cloud
- APIs
- autocomplete
- filters
- analytics
- multi-user

## 13. Acceptance Criteria (MVP Done)
- can save request
- can see list
- can open detail
- can send WhatsApp message
- can mark delivered

## 14. Final Rule
If a feature does not directly help:
👉 save request
👉 notify customer
👉 mark delivered

Do not build it.

