# Mobile 121: registration, login and event conversation

- Registration success starts the existing login flow with transient credentials; no second registration request. Existing captcha, device confirmation and role/bootstrap checks remain in effect. Login failure leaves the login form available.
- New accounts receive one of the 13 existing user avatar images, persisted by the server. User avatar pickers replace the name-initial choice with a random library choice; existing custom avatars are preserved.
- Event conversations have one scroll owner. Rich text is converted once in batches after the popup entrance transition; closing cancels pending work.
- iOS portable SM2 uses 16-bit schoolbook multiplication and reduction by the SM2 field identity. Random nonces and C1C2C3 protocol are unchanged. A BigInt oracle checked 1,232 products including boundary/carry/borrow cases; randomized encrypted passwords decrypt with sm-crypto.
- Composer orb no longer shrinks or applies an additional round image clip.

Validation: 67 APP tests; APP syntax; Android resource build and generated Kotlin/native compile; iOS resource build; backend compile and H2 account regression; web production build. Browser fixtures verify registration issues one registration and one login then navigates to chat, popup open/close cancellation, single scroll owner and 24x24 orb. Native iPhone performance still needs an installed-package check; no signed package was produced here.

One local Node portable-UTS benchmark: 1497 ms before / 48 ms after, identical ciphertext. This is not an iPhone end-to-end login measurement.

Deployment: backend image aimessage-backend:registration-login-20260923-121, healthy with zero restarts. Only AppAccountService.class patched; jar SHA256 179216ad4fc10717928483abfeae7f59c9e0ec44fceedc727db7d46c8d9a3d99. Web /var/www/aimessage/releases/registration-login-20260923-121/dist. Backups /opt/aimessage/backups/pre-registration-login-20260923-121. No production test accounts created.
