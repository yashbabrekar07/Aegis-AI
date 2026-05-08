export const scenariosSet1 = [
  {
    type: 'True/False',
    sender: 'System',
    message: 'True or False: Your bank will sometimes email you asking to reply with your ATM PIN if there is a security breach.',
    options: [
      { text: 'True, to verify identity', correct: false, feedback: 'Banks never ask for your PIN via email or phone.' },
      { text: 'False, they never ask', correct: true, feedback: 'Correct! Never share your PIN with anyone.' }
    ],
    isTimed: false
  },
  {
    type: 'SMS',
    sender: 'VD-PROMO',
    message: 'Congratulations! Your mobile number won ₹25,00,000 in the lucky draw. Click here to claim your prize: http://claim-prize-today.com',
    options: [
      { text: 'Click to see if it is real', correct: false, feedback: 'This is a classic lottery scam using greed tactics.' },
      { text: 'Ignore and delete', correct: true, feedback: 'Spot on! If you didn\'t enter a lottery, you didn\'t win.' }
    ],
    isTimed: false
  },
  {
    type: 'Email',
    sender: 'Netflix Alert',
    message: 'Your account is on hold. Please update your payment details in the next 24 hours.',
    options: [
      { text: 'Log in via the official app to check', correct: true, feedback: 'Always verify through official channels.' },
      { text: 'Reply to the email asking for details', correct: false, feedback: 'Never reply to suspicious emails with personal info.' }
    ],
    isTimed: false
  },
  {
    type: 'WhatsApp (Marathi)',
    sender: 'MSEB Official',
    message: 'Priya grahak, tumche light bill update nahi zale ahe. Aaj ratri 9:30 vajta tumchi light cut keli jail. Tondit bill bharanyasathi link var click kara: http://mseb-update-quick.in',
    options: [
      { text: 'Pay the bill immediately via the link', correct: false, feedback: 'Threatening to cut power at night is a common MSEB scam tactic.' },
      { text: 'Check the official MSEB app', correct: true, feedback: 'Excellent. Official electricity boards never send unverified SMS links threatening immediate disconnection.' }
    ],
    isTimed: false
  },
  {
    type: 'WhatsApp',
    sender: 'Colleague (Amit)',
    message: 'Hey, I am stuck in a meeting and urgently need to send Apple gift cards to a client. Can you buy 2 cards of ₹5000 and send me the codes? I will Gpay you back in an hour.',
    options: [
      { text: 'Buy them to help a colleague', correct: false, feedback: 'Classic CEO/Colleague fraud. Always call to verify.' },
      { text: 'Call Amit directly to confirm', correct: true, feedback: 'Great! Voice verification beats text impersonation.' }
    ],
    isTimed: false
  },
  {
    type: 'Find the Flaw',
    sender: 'Customer Support <amzon-support@hotmail.com>',
    message: 'Dear Customer, your recent order #88291 has been cancelled. Please click the link to claim your refund.',
    options: [
      { text: 'The order number is too short', correct: false, feedback: 'The sender domain (@hotmail.com) is the real red flag.' },
      { text: 'The sender email domain is generic (@hotmail.com)', correct: true, feedback: 'Official emails use corporate domains, not Hotmail or Gmail.' },
      { text: 'The greeting "Dear Customer"', correct: false, feedback: 'While generic greetings are bad, the hotmail address is the critical flaw.' }
    ],
    isTimed: false
  },
  {
    type: 'Voice Call (Transcript)',
    sender: 'Unknown Number (+92 ...)',
    message: '"Namaskar! Main Kaun Banega Crorepati WhatsApp department se bol raha hu. Aapke number par 25 lakh ki lottery lagi hai. Tax processing ke liye 15,000 UPI kijiye."',
    options: [
      { text: 'Pay the tax to get 25 Lakhs', correct: false, feedback: 'Scammers demand upfront fees for fake prizes.' },
      { text: 'Ask for official ID proof', correct: false, feedback: 'They will send fake IDs. It\'s better to just disconnect.' },
      { text: 'Disconnect immediately', correct: true, feedback: 'Perfect response to the KBC lottery scam.' }
    ],
    isTimed: false
  },
  {
    type: 'Rank the Risk',
    sender: 'Various',
    message: 'Which of these elements makes an email MOST dangerous?',
    options: [
      { text: 'A spelling mistake in the body', correct: false, feedback: 'Typos are common, malicious links are the real weapon.' },
      { text: 'A link asking for login credentials', correct: true, feedback: 'Credential harvesting links are the primary goal of phishing.' },
      { text: 'An unknown sender address', correct: false, feedback: 'Unknown senders are suspicious, but the link causes the actual harm.' }
    ],
    isTimed: false
  },
  {
    type: 'Impersonation Chat',
    sender: 'Dad (New Number)',
    message: 'Hi beta, I dropped my phone in the toilet. This is my temporary number. I urgently need to pay a hospital bill of ₹12,000 for a friend. Can you UPI to this number right now? It\'s an emergency.',
    options: [
      { text: 'Send the money immediately', correct: false, feedback: 'Emergency + New Number = Impersonation Scam.' },
      { text: 'Ask "What is my pet\'s name?"', correct: true, feedback: 'A quick security question exposes the scammer immediately.' }
    ],
    isTimed: true
  },
  {
    type: 'SMS Notification',
    sender: 'V-HDFCBK',
    message: 'Dear Customer, your NetBanking will be BLOCKED in 15 mins due to pending KYC. Update PAN immediately: http://hdfc.kyc-update-net.com',
    options: [
      { text: 'Click the link out of panic', correct: false, feedback: 'Panic makes you miss the fake URL: hdfc.kyc-update-net.com' },
      { text: 'Do nothing', correct: true, feedback: 'Banks never threaten blockages with 15-minute deadlines.' }
    ],
    isTimed: true
  }
];

export const scenariosSet2 = [
  {
    type: 'True/False',
    sender: 'System',
    message: 'True or False: The Income Tax Department sends SMS links to claim your tax refund.',
    options: [
      { text: 'True', correct: false, feedback: 'The IT Department never sends links via SMS asking for bank details for refunds.' },
      { text: 'False', correct: true, feedback: 'Correct! Tax refunds are processed automatically to linked accounts.' }
    ],
    isTimed: false
  },
  {
    type: 'Email',
    sender: 'HR Department <hr@company-update.com>',
    message: 'Mandatory: Complete your annual security training by EOD or your access will be suspended. Click here: bit.ly/sec-train-44',
    options: [
      { text: 'Click immediately to keep access', correct: false, feedback: 'Urgency + shortened link + spoofed domain = Phishing.' },
      { text: 'Verify with HR directly', correct: true, feedback: 'Always verify threatening internal emails via a different channel.' }
    ],
    isTimed: false
  },
  {
    type: 'SMS (Hindi)',
    sender: 'SBI-ALERT',
    message: 'Priya grahak, aapka SBI Yono account block kar diya gaya hai. Apna PAN card update karne ke liye link par click karein: http://sbi-pan-kyc.in',
    options: [
      { text: 'Click link to unblock Yono', correct: false, feedback: 'Fake domains and urgency are hallmarks of SMS phishing.' },
      { text: 'Open the Yono app to check', correct: true, feedback: 'Always use the official app to verify account status.' }
    ],
    isTimed: false
  },
  {
    type: 'WhatsApp',
    sender: '+1 (555) ... (Unknown)',
    message: 'Hello, we are hiring for part-time work from home. Just like YouTube videos and earn ₹5000/day. Interested?',
    options: [
      { text: 'Reply to know more', correct: false, feedback: 'This is a task scam. They will eventually ask you to "invest" money to get bigger tasks.' },
      { text: 'Block and report', correct: true, feedback: 'Excellent. Easy money offers from unknown numbers are always scams.' }
    ],
    isTimed: false
  },
  {
    type: 'Find the Flaw',
    sender: 'Paypal Service <paypal-update@gmail.com>',
    message: 'Your payment of $499.99 to BestBuy was successful. If you did not authorize this, call 1-800-FAKE-NUM immediately to cancel.',
    options: [
      { text: 'The amount is too high', correct: false, feedback: 'The sender domain (@gmail.com) is the red flag.' },
      { text: 'The sender is using a @gmail.com address', correct: true, feedback: 'Paypal uses @paypal.com, never free email providers.' },
      { text: 'Providing a phone number to call', correct: false, feedback: 'While the phone number is part of the scam, the gmail domain proves it is fake.' }
    ],
    isTimed: false
  },
  {
    type: 'Voice Call (Transcript)',
    sender: 'Customs Officer',
    message: '"This is Mumbai Customs. A FedEx package in your name has been seized containing illegal passports. Press 1 to speak to an officer or an arrest warrant will be issued."',
    options: [
      { text: 'Press 1 to explain the mistake', correct: false, feedback: 'Pressing 1 connects you to the scammer who will extort money.' },
      { text: 'Hang up the phone', correct: true, feedback: 'Perfect. Law enforcement does not issue arrest warrants via automated robocalls.' }
    ],
    isTimed: false
  },
  {
    type: 'Scenario',
    sender: 'Friend on Facebook',
    message: 'Hey bro, I am stuck at the hospital and my card isn\'t working. Can you Gpay me 10k right now? I\'ll send it back tomorrow morning.',
    options: [
      { text: 'Send the money, it is a medical emergency', correct: false, feedback: 'Their account was likely hacked. Never send money based on a text message.' },
      { text: 'Call them on their phone to confirm', correct: true, feedback: 'Voice verification is the best defense against account takeover scams.' }
    ],
    isTimed: false
  },
  {
    type: 'Rank the Risk',
    sender: 'System',
    message: 'Which of the following is an example of Advance Fee Fraud?',
    options: [
      { text: 'A fake login page for Netflix', correct: false, feedback: 'That is phishing.' },
      { text: 'Winning a free iPhone but having to pay ₹500 for "shipping"', correct: true, feedback: 'Scammers collect the "fee" and disappear. There is no prize.' },
      { text: 'A text message from your boss asking for a favor', correct: false, feedback: 'That is CEO/Impersonation fraud.' }
    ],
    isTimed: false
  },
  {
    type: 'Impersonation Chat',
    sender: 'CEO (Anil)',
    message: 'I am in a board meeting and cannot talk. I need you to initiate a wire transfer of ₹2 Lakhs to a new vendor immediately. Details attached.',
    options: [
      { text: 'Process the transfer to look efficient', correct: false, feedback: 'Never bypass financial protocols, even for the CEO.' },
      { text: 'Wait for the meeting to end and verify', correct: true, feedback: 'Correct! Business Email Compromise (BEC) relies on bypassing verification.' }
    ],
    isTimed: true
  },
  {
    type: 'SMS Notification',
    sender: 'NHAI-FASTAG',
    message: 'Your FASTag has been BLACKLISTED due to low balance. Recharge immediately via this link to avoid ₹1000 penalty at next toll: bit.ly/fastag-update',
    options: [
      { text: 'Recharge via the link to avoid penalty', correct: false, feedback: 'Urgency + shortened link = FASTag scam.' },
      { text: 'Check the official FASTag app or bank portal', correct: true, feedback: 'Always verify through your official banking app.' }
    ],
    isTimed: true
  }
];

export const scenariosSet3 = [
  {
    type: 'True/False',
    sender: 'System',
    message: 'True or False: Scammers can make their phone number appear as the official Police or Bank number on your caller ID.',
    options: [
      { text: 'False, caller ID is secure', correct: false, feedback: 'Caller ID spoofing is very easy and common.' },
      { text: 'True', correct: true, feedback: 'Correct! This is called Caller ID Spoofing.' }
    ],
    isTimed: false
  },
  {
    type: 'Email',
    sender: 'Google Storage <alert@google-storage-update.info>',
    message: 'Your Gmail storage is 99% full. You will stop receiving emails in 2 hours. Click here to upgrade your storage for free.',
    options: [
      { text: 'Click to upgrade for free', correct: false, feedback: 'Google does not give free upgrades via suspicious domains.' },
      { text: 'Check storage in the Google One app', correct: true, feedback: 'Always verify directly in the application.' }
    ],
    isTimed: false
  },
  {
    type: 'SMS',
    sender: 'E-CHALLAN',
    message: 'Your vehicle has a pending traffic violation challan of ₹2000. Pay immediately to avoid court case: http://e-challan-parivahan.in.net',
    options: [
      { text: 'Pay the challan to avoid court', correct: false, feedback: 'The URL uses a fake domain (.in.net). Official sites use .gov.in.' },
      { text: 'Ignore or check official parivahan.gov.in', correct: true, feedback: 'Always look for the official .gov.in domain.' }
    ],
    isTimed: false
  },
  {
    type: 'WhatsApp (Hinglish)',
    sender: 'Friend (New Number)',
    message: 'Bhai Paytm par 500 bhej de, mera daily limit cross ho gaya hai aur petrol pump par hu. Ghar jaake wapas karta hu.',
    options: [
      { text: 'Send the money to help him out', correct: false, feedback: 'Never send money to a new number claiming to be a friend without voice confirmation.' },
      { text: 'Call the friend on his old number', correct: true, feedback: 'Excellent. He will likely pick up and tell you he is not at a petrol pump.' }
    ],
    isTimed: false
  },
  {
    type: 'Find the Flaw',
    sender: 'Vendor Billing',
    message: 'Please find the attached urgent invoice for this month. \nAttachment: INVOICE_MAY_2023.pdf.exe',
    options: [
      { text: 'The date is wrong', correct: false, feedback: 'The file extension is the critical danger.' },
      { text: 'The file ends in .exe instead of .pdf', correct: true, feedback: 'An .exe file is an executable program (malware), not a document.' },
      { text: 'There is no greeting', correct: false, feedback: 'While unprofessional, the executable file is the actual threat.' }
    ],
    isTimed: false
  },
  {
    type: 'Voice Call (Transcript)',
    sender: 'Telecom Dept (TRAI)',
    message: '"This is the Telecom Department. Your Aadhaar has been used to register 15 illegal SIM cards used for money laundering. Press 9 to connect to the CBI officer."',
    options: [
      { text: 'Press 9 to clear your name', correct: false, feedback: 'TRAI does not call citizens directly. Pressing 9 connects you to the fake "CBI" extortionist.' },
      { text: 'Disconnect immediately', correct: true, feedback: 'Correct! This is a massive extortion scam currently active.' }
    ],
    isTimed: false
  },
  {
    type: 'Scenario',
    sender: 'Physical World',
    message: 'You find a USB drive in the company parking lot labeled "Confidential Salary Data". What do you do?',
    options: [
      { text: 'Plug it in to see who it belongs to', correct: false, feedback: 'USB drops are a common physical hacking technique to deploy malware.' },
      { text: 'Give it to the IT Security team', correct: true, feedback: 'Perfect. Never plug untrusted media into your devices.' }
    ],
    isTimed: false
  },
  {
    type: 'Rank the Risk',
    sender: 'System',
    message: 'Which password practice is the MOST dangerous?',
    options: [
      { text: 'Using a password that is only 8 characters long', correct: false, feedback: 'While weak, reusing passwords is worse.' },
      { text: 'Reusing the same password across your email and banking sites', correct: true, feedback: 'If one site is breached, hackers will test the password on all your critical accounts.' },
      { text: 'Writing your password down on a paper at home', correct: false, feedback: 'Physical theft is lower risk than remote credential stuffing.' }
    ],
    isTimed: false
  },
  {
    type: 'Impersonation Chat',
    sender: 'Tech Support (Microsoft)',
    message: 'Hi, we have detected malicious network traffic coming from your IP address. Your computer is infected. Please download AnyDesk so our technician can remove the virus.',
    options: [
      { text: 'Download AnyDesk to fix the virus', correct: false, feedback: 'AnyDesk gives them full control of your PC and bank accounts.' },
      { text: 'Close the chat and ignore', correct: true, Microsoft: 'Microsoft does not proactively reach out to fix your PC.' }
    ],
    isTimed: true
  },
  {
    type: 'SMS Notification',
    sender: 'APK-ALERT',
    message: 'Your electricity bill for last month is updated. Please download our new billing app directly from this link to pay: http://mseb-app-download.com/app.apk',
    options: [
      { text: 'Download the APK to pay', correct: false, feedback: 'Sideloading APKs from SMS links will install spyware that reads your OTPs.' },
      { text: 'Only download apps from the official Play Store / App Store', correct: true, feedback: 'Always stick to official app stores.' }
    ],
    isTimed: true
  }
];
