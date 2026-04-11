# Medical Shop Request Manager (MVP) - User Stories

## Story 1: Save a medicine request
**As a** medical shop worker, **I want** to quickly save a customer request with name, phone, and medicine, **so that** I do not forget who asked for what.

### Acceptance Criteria
- Worker can enter `Name`, `Phone`, and `Medicine` on the Home screen.
- Tapping `Save` stores a new request locally.
- New request is saved with status `PENDING` by default.

## Story 2: Validate required fields
**As a** medical shop worker, **I want** basic required-field checks before saving, **so that** incomplete requests are not stored.

### Acceptance Criteria
- If `Name` is empty, request is not saved.
- If `Phone` is empty, request is not saved.
- If `Medicine` is empty, request is not saved.
- Request is saved only when all three fields are non-empty.

## Story 3: View request list with status
**As a** medical shop worker, **I want** to see all saved requests in one list, **so that** I can quickly track pending and delivered items.

### Acceptance Criteria
- Request List screen shows all saved requests.
- Each list item shows `Medicine`, `Name`, `Phone`, and `Status`.
- Status is visible as `PENDING` or `DELIVERED`.

## Story 4: Open request details from the list
**As a** medical shop worker, **I want** to open a request from the list, **so that** I can take action for that customer.

### Acceptance Criteria
- Tapping a request in the list opens Request Detail screen.
- Detail screen shows `Name`, `Phone`, and `Medicine` for the selected request.
- Detail screen includes `Send Msg` and `Delivered` actions.

## Story 5: Choose contact method from Send Msg
**As a** medical shop worker, **I want** to choose WhatsApp, SMS, or Call after tapping `Send Msg`, **so that** I can contact the customer using the best available option.

### Acceptance Criteria
- Tapping `Send Msg` opens a simple bottom sheet (or dialog).
- Sheet/dialog shows exactly 3 options: `WhatsApp`, `SMS`, `Call`.
- No extra styling or non-MVP actions are added.

## Story 6: Send WhatsApp message from chooser
**As a** medical shop worker, **I want** to send the availability message through WhatsApp, **so that** I can notify customers quickly.

### Acceptance Criteria
- Selecting `WhatsApp` uses the existing saved-settings message template logic (no duplicate message generation logic).
- WhatsApp is opened using: `https://wa.me/91{phone}?text={encodedMessage}`.
- Message text is properly URL encoded before opening WhatsApp.
- If WhatsApp is not installed, app shows toast: `WhatsApp not installed`.
- If phone number is empty, app shows toast: `Invalid phone number`.

## Story 7: Send SMS message from chooser
**As a** medical shop worker, **I want** to open SMS with a prefilled message, **so that** I can contact customers when WhatsApp is not preferred.

### Acceptance Criteria
- Selecting `SMS` opens default SMS app using `Intent.ACTION_VIEW` with `sms:{phone}`.
- SMS body is prefilled using extra `sms_body`.
- SMS message uses the same existing saved-settings template logic.
- If phone number is empty, app shows toast: `Invalid phone number`.

## Story 8: Open phone dialer from chooser
**As a** medical shop worker, **I want** to open the dialer with customer number prefilled, **so that** I can place a call manually.

### Acceptance Criteria
- Selecting `Call` opens phone dialer using `Intent.ACTION_DIAL` with `tel:{phone}`.
- App does not directly place a call and does not require CALL permission.
- If phone number is empty, app shows toast: `Invalid phone number`.

## Story 9: Mark request as delivered
**As a** medical shop worker, **I want** to mark a request as delivered, **so that** pending work stays accurate.

### Acceptance Criteria
- Tapping `Delivered` changes request status from `PENDING` to `DELIVERED`.
- Updated status is visible in Request Detail.
- Updated status is visible in Request List.

## Story 10: Keep the app simple, offline, and fast
**As a** medical shop worker, **I want** the app to work offline and open quickly with a simple UI, **so that** I can use it reliably during shop work.

### Acceptance Criteria
- Core flow works without internet: save, list, detail, message intent launch, and delivered update.
- App launches quickly enough for day-to-day counter use.
- UI contains only MVP screens and actions from the PRD scope.

## Out of Scope Guardrail
The MVP does **not** include login, backend/cloud sync, external APIs, autocomplete, filters, analytics, or multi-user support.
