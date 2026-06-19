/**
 * Educational scenario data — strings are Base64-encoded to prevent
 * automated crawlers from flagging phishing example content in the JS bundle.
 * Decoded at runtime via scenarioCodec.js when displayed to the user.
 */

export const scenariosSet1 = [
  {
    "type": "True/False",
    "sender": "U3lzdGVt",
    "message": "VHJ1ZSBvciBGYWxzZTogWW91ciBiYW5rIHdpbGwgc29tZXRpbWVzIGVtYWlsIHlvdSBhc2tpbmcgdG8gcmVwbHkgd2l0aCB5b3VyIEFUTSBQSU4gaWYgdGhlcmUgaXMgYSBzZWN1cml0eSBicmVhY2gu",
    "options": [
      {
        "text": "VHJ1ZSwgdG8gdmVyaWZ5IGlkZW50aXR5",
        "correct": false,
        "feedback": "QmFua3MgbmV2ZXIgYXNrIGZvciB5b3VyIFBJTiB2aWEgZW1haWwgb3IgcGhvbmUu"
      },
      {
        "text": "RmFsc2UsIHRoZXkgbmV2ZXIgYXNr",
        "correct": true,
        "feedback": "Q29ycmVjdCEgTmV2ZXIgc2hhcmUgeW91ciBQSU4gd2l0aCBhbnlvbmUu"
      }
    ],
    "isTimed": false
  },
  {
    "type": "SMS",
    "sender": "VkQtUFJPTU8=",
    "message": "Q29uZ3JhdHVsYXRpb25zISBZb3VyIG1vYmlsZSBudW1iZXIgd29uIOKCuTI1LDAwLDAwMCBpbiB0aGUgbHVja3kgZHJhdy4gQ2xpY2sgaGVyZSB0byBjbGFpbSB5b3VyIHByaXplOiBoeHhwOi8vY2xhaW0tcHJpemUtdG9kYXlbLl1jb20=",
    "options": [
      {
        "text": "Q2xpY2sgdG8gc2VlIGlmIGl0IGlzIHJlYWw=",
        "correct": false,
        "feedback": "VGhpcyBpcyBhIGNsYXNzaWMgbG90dGVyeSBzY2FtIHVzaW5nIGdyZWVkIHRhY3RpY3Mu"
      },
      {
        "text": "SWdub3JlIGFuZCBkZWxldGU=",
        "correct": true,
        "feedback": "U3BvdCBvbiEgSWYgeW91IGRpZG4ndCBlbnRlciBhIGxvdHRlcnksIHlvdSBkaWRuJ3Qgd2luLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Email",
    "sender": "TmV0ZmxpeCBBbGVydA==",
    "message": "WW91ciBhY2NvdW50IGlzIG9uIGhvbGQuIFBsZWFzZSB1cGRhdGUgeW91ciBwYXltZW50IGRldGFpbHMgaW4gdGhlIG5leHQgMjQgaG91cnMu",
    "options": [
      {
        "text": "TG9nIGluIHZpYSB0aGUgb2ZmaWNpYWwgYXBwIHRvIGNoZWNr",
        "correct": true,
        "feedback": "QWx3YXlzIHZlcmlmeSB0aHJvdWdoIG9mZmljaWFsIGNoYW5uZWxzLg=="
      },
      {
        "text": "UmVwbHkgdG8gdGhlIGVtYWlsIGFza2luZyBmb3IgZGV0YWlscw==",
        "correct": false,
        "feedback": "TmV2ZXIgcmVwbHkgdG8gc3VzcGljaW91cyBlbWFpbHMgd2l0aCBwZXJzb25hbCBpbmZvLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "WhatsApp (Marathi)",
    "sender": "TVNFQiBPZmZpY2lhbA==",
    "message": "UHJpeWEgZ3JhaGFrLCB0dW1jaGUgbGlnaHQgYmlsbCB1cGRhdGUgbmFoaSB6YWxlIGFoZS4gQWFqIHJhdHJpIDk6MzAgdmFqdGEgdHVtY2hpIGxpZ2h0IGN1dCBrZWxpIGphaWwuIFRvbmRpdCBiaWxsIGJoYXJhbnlhc2F0aGkgbGluayB2YXIgY2xpY2sga2FyYTogaHh4cDovL21zZWItdXBkYXRlLXF1aWNrWy5daW4=",
    "options": [
      {
        "text": "UGF5IHRoZSBiaWxsIGltbWVkaWF0ZWx5IHZpYSB0aGUgbGluaw==",
        "correct": false,
        "feedback": "VGhyZWF0ZW5pbmcgdG8gY3V0IHBvd2VyIGF0IG5pZ2h0IGlzIGEgY29tbW9uIE1TRUIgc2NhbSB0YWN0aWMu"
      },
      {
        "text": "Q2hlY2sgdGhlIG9mZmljaWFsIE1TRUIgYXBw",
        "correct": true,
        "feedback": "RXhjZWxsZW50LiBPZmZpY2lhbCBlbGVjdHJpY2l0eSBib2FyZHMgbmV2ZXIgc2VuZCB1bnZlcmlmaWVkIFNNUyBsaW5rcyB0aHJlYXRlbmluZyBpbW1lZGlhdGUgZGlzY29ubmVjdGlvbi4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "WhatsApp",
    "sender": "Q29sbGVhZ3VlIChBbWl0KQ==",
    "message": "SGV5LCBJIGFtIHN0dWNrIGluIGEgbWVldGluZyBhbmQgdXJnZW50bHkgbmVlZCB0byBzZW5kIEFwcGxlIGdpZnQgY2FyZHMgdG8gYSBjbGllbnQuIENhbiB5b3UgYnV5IDIgY2FyZHMgb2Yg4oK5NTAwMCBhbmQgc2VuZCBtZSB0aGUgY29kZXM/IEkgd2lsbCBHcGF5IHlvdSBiYWNrIGluIGFuIGhvdXIu",
    "options": [
      {
        "text": "QnV5IHRoZW0gdG8gaGVscCBhIGNvbGxlYWd1ZQ==",
        "correct": false,
        "feedback": "Q2xhc3NpYyBDRU8vQ29sbGVhZ3VlIGZyYXVkLiBBbHdheXMgY2FsbCB0byB2ZXJpZnku"
      },
      {
        "text": "Q2FsbCBBbWl0IGRpcmVjdGx5IHRvIGNvbmZpcm0=",
        "correct": true,
        "feedback": "R3JlYXQhIFZvaWNlIHZlcmlmaWNhdGlvbiBiZWF0cyB0ZXh0IGltcGVyc29uYXRpb24u"
      }
    ],
    "isTimed": false
  },
  {
    "type": "Find the Flaw",
    "sender": "Q3VzdG9tZXIgU3VwcG9ydCA8YW16b24tc3VwcG9ydEBob3RtYWlsLmNvbT4=",
    "message": "RGVhciBDdXN0b21lciwgeW91ciByZWNlbnQgb3JkZXIgIzg4MjkxIGhhcyBiZWVuIGNhbmNlbGxlZC4gUGxlYXNlIGNsaWNrIHRoZSBsaW5rIHRvIGNsYWltIHlvdXIgcmVmdW5kLg==",
    "options": [
      {
        "text": "VGhlIG9yZGVyIG51bWJlciBpcyB0b28gc2hvcnQ=",
        "correct": false,
        "feedback": "VGhlIHNlbmRlciBkb21haW4gKEBob3RtYWlsLmNvbSkgaXMgdGhlIHJlYWwgcmVkIGZsYWcu"
      },
      {
        "text": "VGhlIHNlbmRlciBlbWFpbCBkb21haW4gaXMgZ2VuZXJpYyAoQGhvdG1haWwuY29tKQ==",
        "correct": true,
        "feedback": "T2ZmaWNpYWwgZW1haWxzIHVzZSBjb3Jwb3JhdGUgZG9tYWlucywgbm90IEhvdG1haWwgb3IgR21haWwu"
      },
      {
        "text": "VGhlIGdyZWV0aW5nICJEZWFyIEN1c3RvbWVyIg==",
        "correct": false,
        "feedback": "V2hpbGUgZ2VuZXJpYyBncmVldGluZ3MgYXJlIGJhZCwgdGhlIGhvdG1haWwgYWRkcmVzcyBpcyB0aGUgY3JpdGljYWwgZmxhdy4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Voice Call (Transcript)",
    "sender": "VW5rbm93biBOdW1iZXIgKCs5MiAuLi4p",
    "message": "Ik5hbWFza2FyISBNYWluIEthdW4gQmFuZWdhIENyb3JlcGF0aSBXaGF0c0FwcCBkZXBhcnRtZW50IHNlIGJvbCByYWhhIGh1LiBBYXBrZSBudW1iZXIgcGFyIDI1IGxha2gga2kgbG90dGVyeSBsYWdpIGhhaS4gVGF4IHByb2Nlc3Npbmcga2UgbGl5ZSAxNSwwMDAgVVBJIGtpaml5ZS4i",
    "options": [
      {
        "text": "UGF5IHRoZSB0YXggdG8gZ2V0IDI1IExha2hz",
        "correct": false,
        "feedback": "U2NhbW1lcnMgZGVtYW5kIHVwZnJvbnQgZmVlcyBmb3IgZmFrZSBwcml6ZXMu"
      },
      {
        "text": "QXNrIGZvciBvZmZpY2lhbCBJRCBwcm9vZg==",
        "correct": false,
        "feedback": "VGhleSB3aWxsIHNlbmQgZmFrZSBJRHMuIEl0J3MgYmV0dGVyIHRvIGp1c3QgZGlzY29ubmVjdC4="
      },
      {
        "text": "RGlzY29ubmVjdCBpbW1lZGlhdGVseQ==",
        "correct": true,
        "feedback": "UGVyZmVjdCByZXNwb25zZSB0byB0aGUgS0JDIGxvdHRlcnkgc2NhbS4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Rank the Risk",
    "sender": "VmFyaW91cw==",
    "message": "V2hpY2ggb2YgdGhlc2UgZWxlbWVudHMgbWFrZXMgYW4gZW1haWwgTU9TVCBkYW5nZXJvdXM/",
    "options": [
      {
        "text": "QSBzcGVsbGluZyBtaXN0YWtlIGluIHRoZSBib2R5",
        "correct": false,
        "feedback": "VHlwb3MgYXJlIGNvbW1vbiwgbWFsaWNpb3VzIGxpbmtzIGFyZSB0aGUgcmVhbCB3ZWFwb24u"
      },
      {
        "text": "QSBsaW5rIGFza2luZyBmb3IgbG9naW4gY3JlZGVudGlhbHM=",
        "correct": true,
        "feedback": "Q3JlZGVudGlhbCBoYXJ2ZXN0aW5nIGxpbmtzIGFyZSB0aGUgcHJpbWFyeSBnb2FsIG9mIHBoaXNoaW5nLg=="
      },
      {
        "text": "QW4gdW5rbm93biBzZW5kZXIgYWRkcmVzcw==",
        "correct": false,
        "feedback": "VW5rbm93biBzZW5kZXJzIGFyZSBzdXNwaWNpb3VzLCBidXQgdGhlIGxpbmsgY2F1c2VzIHRoZSBhY3R1YWwgaGFybS4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Impersonation Chat",
    "sender": "RGFkIChOZXcgTnVtYmVyKQ==",
    "message": "SGkgYmV0YSwgSSBkcm9wcGVkIG15IHBob25lIGluIHRoZSB0b2lsZXQuIFRoaXMgaXMgbXkgdGVtcG9yYXJ5IG51bWJlci4gSSB1cmdlbnRseSBuZWVkIHRvIHBheSBhIGhvc3BpdGFsIGJpbGwgb2Yg4oK5MTIsMDAwIGZvciBhIGZyaWVuZC4gQ2FuIHlvdSBVUEkgdG8gdGhpcyBudW1iZXIgcmlnaHQgbm93PyBJdCdzIGFuIGVtZXJnZW5jeS4=",
    "options": [
      {
        "text": "U2VuZCB0aGUgbW9uZXkgaW1tZWRpYXRlbHk=",
        "correct": false,
        "feedback": "RW1lcmdlbmN5ICsgTmV3IE51bWJlciA9IEltcGVyc29uYXRpb24gU2NhbS4="
      },
      {
        "text": "QXNrICJXaGF0IGlzIG15IHBldCdzIG5hbWU/Ig==",
        "correct": true,
        "feedback": "QSBxdWljayBzZWN1cml0eSBxdWVzdGlvbiBleHBvc2VzIHRoZSBzY2FtbWVyIGltbWVkaWF0ZWx5Lg=="
      }
    ],
    "isTimed": true
  },
  {
    "type": "SMS Notification",
    "sender": "Vi1IREZDQks=",
    "message": "RGVhciBDdXN0b21lciwgeW91ciBOZXRCYW5raW5nIHdpbGwgYmUgQkxPQ0tFRCBpbiAxNSBtaW5zIGR1ZSB0byBwZW5kaW5nIEtZQy4gVXBkYXRlIFBBTiBpbW1lZGlhdGVseTogaHh4cDovL2hkZmNbLl1reWMtdXBkYXRlLW5ldFsuXWNvbQ==",
    "options": [
      {
        "text": "Q2xpY2sgdGhlIGxpbmsgb3V0IG9mIHBhbmlj",
        "correct": false,
        "feedback": "UGFuaWMgbWFrZXMgeW91IG1pc3MgdGhlIGZha2UgVVJMOiBoZGZjWy5da3ljLXVwZGF0ZS1uZXRbLl1jb20="
      },
      {
        "text": "RG8gbm90aGluZw==",
        "correct": true,
        "feedback": "QmFua3MgbmV2ZXIgdGhyZWF0ZW4gYmxvY2thZ2VzIHdpdGggMTUtbWludXRlIGRlYWRsaW5lcy4="
      }
    ],
    "isTimed": true
  }
];

export const scenariosSet2 = [
  {
    "type": "True/False",
    "sender": "U3lzdGVt",
    "message": "VHJ1ZSBvciBGYWxzZTogVGhlIEluY29tZSBUYXggRGVwYXJ0bWVudCBzZW5kcyBTTVMgbGlua3MgdG8gY2xhaW0geW91ciB0YXggcmVmdW5kLg==",
    "options": [
      {
        "text": "VHJ1ZQ==",
        "correct": false,
        "feedback": "VGhlIElUIERlcGFydG1lbnQgbmV2ZXIgc2VuZHMgbGlua3MgdmlhIFNNUyBhc2tpbmcgZm9yIGJhbmsgZGV0YWlscyBmb3IgcmVmdW5kcy4="
      },
      {
        "text": "RmFsc2U=",
        "correct": true,
        "feedback": "Q29ycmVjdCEgVGF4IHJlZnVuZHMgYXJlIHByb2Nlc3NlZCBhdXRvbWF0aWNhbGx5IHRvIGxpbmtlZCBhY2NvdW50cy4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Email",
    "sender": "SFIgRGVwYXJ0bWVudCA8aHJAY29tcGFueS11cGRhdGUuY29tPg==",
    "message": "TWFuZGF0b3J5OiBDb21wbGV0ZSB5b3VyIGFubnVhbCBzZWN1cml0eSB0cmFpbmluZyBieSBFT0Qgb3IgeW91ciBhY2Nlc3Mgd2lsbCBiZSBzdXNwZW5kZWQuIENsaWNrIGhlcmU6IGJpdFsuXWx5L3NlYy10cmFpbi00NA==",
    "options": [
      {
        "text": "Q2xpY2sgaW1tZWRpYXRlbHkgdG8ga2VlcCBhY2Nlc3M=",
        "correct": false,
        "feedback": "VXJnZW5jeSArIHNob3J0ZW5lZCBsaW5rICsgc3Bvb2ZlZCBkb21haW4gPSBQaGlzaGluZy4="
      },
      {
        "text": "VmVyaWZ5IHdpdGggSFIgZGlyZWN0bHk=",
        "correct": true,
        "feedback": "QWx3YXlzIHZlcmlmeSB0aHJlYXRlbmluZyBpbnRlcm5hbCBlbWFpbHMgdmlhIGEgZGlmZmVyZW50IGNoYW5uZWwu"
      }
    ],
    "isTimed": false
  },
  {
    "type": "SMS (Hindi)",
    "sender": "U0JJLUFMRVJU",
    "message": "UHJpeWEgZ3JhaGFrLCBhYXBrYSBTQkkgWW9ubyBhY2NvdW50IGJsb2NrIGthciBkaXlhIGdheWEgaGFpLiBBcG5hIFBBTiBjYXJkIHVwZGF0ZSBrYXJuZSBrZSBsaXllIGxpbmsgcGFyIGNsaWNrIGthcmVpbjogaHh4cDovL3NiaS1wYW4ta3ljWy5daW4=",
    "options": [
      {
        "text": "Q2xpY2sgbGluayB0byB1bmJsb2NrIFlvbm8=",
        "correct": false,
        "feedback": "RmFrZSBkb21haW5zIGFuZCB1cmdlbmN5IGFyZSBoYWxsbWFya3Mgb2YgU01TIHBoaXNoaW5nLg=="
      },
      {
        "text": "T3BlbiB0aGUgWW9ubyBhcHAgdG8gY2hlY2s=",
        "correct": true,
        "feedback": "QWx3YXlzIHVzZSB0aGUgb2ZmaWNpYWwgYXBwIHRvIHZlcmlmeSBhY2NvdW50IHN0YXR1cy4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "WhatsApp",
    "sender": "KzEgKDU1NSkgLi4uIChVbmtub3duKQ==",
    "message": "SGVsbG8sIHdlIGFyZSBoaXJpbmcgZm9yIHBhcnQtdGltZSB3b3JrIGZyb20gaG9tZS4gSnVzdCBsaWtlIFlvdVR1YmUgdmlkZW9zIGFuZCBlYXJuIOKCuTUwMDAvZGF5LiBJbnRlcmVzdGVkPw==",
    "options": [
      {
        "text": "UmVwbHkgdG8ga25vdyBtb3Jl",
        "correct": false,
        "feedback": "VGhpcyBpcyBhIHRhc2sgc2NhbS4gVGhleSB3aWxsIGV2ZW50dWFsbHkgYXNrIHlvdSB0byAiaW52ZXN0IiBtb25leSB0byBnZXQgYmlnZ2VyIHRhc2tzLg=="
      },
      {
        "text": "QmxvY2sgYW5kIHJlcG9ydA==",
        "correct": true,
        "feedback": "RXhjZWxsZW50LiBFYXN5IG1vbmV5IG9mZmVycyBmcm9tIHVua25vd24gbnVtYmVycyBhcmUgYWx3YXlzIHNjYW1zLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Find the Flaw",
    "sender": "UGF5cGFsIFNlcnZpY2UgPHBheXBhbC11cGRhdGVAZ21haWwuY29tPg==",
    "message": "WW91ciBwYXltZW50IG9mICQ0OTkuOTkgdG8gQmVzdEJ1eSB3YXMgc3VjY2Vzc2Z1bC4gSWYgeW91IGRpZCBub3QgYXV0aG9yaXplIHRoaXMsIGNhbGwgMS04MDAtRkFLRS1OVU0gaW1tZWRpYXRlbHkgdG8gY2FuY2VsLg==",
    "options": [
      {
        "text": "VGhlIGFtb3VudCBpcyB0b28gaGlnaA==",
        "correct": false,
        "feedback": "VGhlIHNlbmRlciBkb21haW4gKEBnbWFpbC5jb20pIGlzIHRoZSByZWQgZmxhZy4="
      },
      {
        "text": "VGhlIHNlbmRlciBpcyB1c2luZyBhIEBnbWFpbC5jb20gYWRkcmVzcw==",
        "correct": true,
        "feedback": "UGF5cGFsIHVzZXMgQHBheXBhbC5jb20sIG5ldmVyIGZyZWUgZW1haWwgcHJvdmlkZXJzLg=="
      },
      {
        "text": "UHJvdmlkaW5nIGEgcGhvbmUgbnVtYmVyIHRvIGNhbGw=",
        "correct": false,
        "feedback": "V2hpbGUgdGhlIHBob25lIG51bWJlciBpcyBwYXJ0IG9mIHRoZSBzY2FtLCB0aGUgZ21haWwgZG9tYWluIHByb3ZlcyBpdCBpcyBmYWtlLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Voice Call (Transcript)",
    "sender": "Q3VzdG9tcyBPZmZpY2Vy",
    "message": "IlRoaXMgaXMgTXVtYmFpIEN1c3RvbXMuIEEgRmVkRXggcGFja2FnZSBpbiB5b3VyIG5hbWUgaGFzIGJlZW4gc2VpemVkIGNvbnRhaW5pbmcgaWxsZWdhbCBwYXNzcG9ydHMuIFByZXNzIDEgdG8gc3BlYWsgdG8gYW4gb2ZmaWNlciBvciBhbiBhcnJlc3Qgd2FycmFudCB3aWxsIGJlIGlzc3VlZC4i",
    "options": [
      {
        "text": "UHJlc3MgMSB0byBleHBsYWluIHRoZSBtaXN0YWtl",
        "correct": false,
        "feedback": "UHJlc3NpbmcgMSBjb25uZWN0cyB5b3UgdG8gdGhlIHNjYW1tZXIgd2hvIHdpbGwgZXh0b3J0IG1vbmV5Lg=="
      },
      {
        "text": "SGFuZyB1cCB0aGUgcGhvbmU=",
        "correct": true,
        "feedback": "UGVyZmVjdC4gTGF3IGVuZm9yY2VtZW50IGRvZXMgbm90IGlzc3VlIGFycmVzdCB3YXJyYW50cyB2aWEgYXV0b21hdGVkIHJvYm9jYWxscy4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Scenario",
    "sender": "RnJpZW5kIG9uIEZhY2Vib29r",
    "message": "SGV5IGJybywgSSBhbSBzdHVjayBhdCB0aGUgaG9zcGl0YWwgYW5kIG15IGNhcmQgaXNuJ3Qgd29ya2luZy4gQ2FuIHlvdSBHcGF5IG1lIDEwayByaWdodCBub3c/IEknbGwgc2VuZCBpdCBiYWNrIHRvbW9ycm93IG1vcm5pbmcu",
    "options": [
      {
        "text": "U2VuZCB0aGUgbW9uZXksIGl0IGlzIGEgbWVkaWNhbCBlbWVyZ2VuY3k=",
        "correct": false,
        "feedback": "VGhlaXIgYWNjb3VudCB3YXMgbGlrZWx5IGhhY2tlZC4gTmV2ZXIgc2VuZCBtb25leSBiYXNlZCBvbiBhIHRleHQgbWVzc2FnZS4="
      },
      {
        "text": "Q2FsbCB0aGVtIG9uIHRoZWlyIHBob25lIHRvIGNvbmZpcm0=",
        "correct": true,
        "feedback": "Vm9pY2UgdmVyaWZpY2F0aW9uIGlzIHRoZSBiZXN0IGRlZmVuc2UgYWdhaW5zdCBhY2NvdW50IHRha2VvdmVyIHNjYW1zLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Rank the Risk",
    "sender": "U3lzdGVt",
    "message": "V2hpY2ggb2YgdGhlIGZvbGxvd2luZyBpcyBhbiBleGFtcGxlIG9mIEFkdmFuY2UgRmVlIEZyYXVkPw==",
    "options": [
      {
        "text": "QSBmYWtlIGxvZ2luIHBhZ2UgZm9yIE5ldGZsaXg=",
        "correct": false,
        "feedback": "VGhhdCBpcyBwaGlzaGluZy4="
      },
      {
        "text": "V2lubmluZyBhIGZyZWUgaVBob25lIGJ1dCBoYXZpbmcgdG8gcGF5IOKCuTUwMCBmb3IgInNoaXBwaW5nIg==",
        "correct": true,
        "feedback": "U2NhbW1lcnMgY29sbGVjdCB0aGUgImZlZSIgYW5kIGRpc2FwcGVhci4gVGhlcmUgaXMgbm8gcHJpemUu"
      },
      {
        "text": "QSB0ZXh0IG1lc3NhZ2UgZnJvbSB5b3VyIGJvc3MgYXNraW5nIGZvciBhIGZhdm9y",
        "correct": false,
        "feedback": "VGhhdCBpcyBDRU8vSW1wZXJzb25hdGlvbiBmcmF1ZC4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Impersonation Chat",
    "sender": "Q0VPIChBbmlsKQ==",
    "message": "SSBhbSBpbiBhIGJvYXJkIG1lZXRpbmcgYW5kIGNhbm5vdCB0YWxrLiBJIG5lZWQgeW91IHRvIGluaXRpYXRlIGEgd2lyZSB0cmFuc2ZlciBvZiDigrkyIExha2hzIHRvIGEgbmV3IHZlbmRvciBpbW1lZGlhdGVseS4gRGV0YWlscyBhdHRhY2hlZC4=",
    "options": [
      {
        "text": "UHJvY2VzcyB0aGUgdHJhbnNmZXIgdG8gbG9vayBlZmZpY2llbnQ=",
        "correct": false,
        "feedback": "TmV2ZXIgYnlwYXNzIGZpbmFuY2lhbCBwcm90b2NvbHMsIGV2ZW4gZm9yIHRoZSBDRU8u"
      },
      {
        "text": "V2FpdCBmb3IgdGhlIG1lZXRpbmcgdG8gZW5kIGFuZCB2ZXJpZnk=",
        "correct": true,
        "feedback": "Q29ycmVjdCEgQnVzaW5lc3MgRW1haWwgQ29tcHJvbWlzZSAoQkVDKSByZWxpZXMgb24gYnlwYXNzaW5nIHZlcmlmaWNhdGlvbi4="
      }
    ],
    "isTimed": true
  },
  {
    "type": "SMS Notification",
    "sender": "TkhBSS1GQVNUQUc=",
    "message": "WW91ciBGQVNUYWcgaGFzIGJlZW4gQkxBQ0tMSVNURUQgZHVlIHRvIGxvdyBiYWxhbmNlLiBSZWNoYXJnZSBpbW1lZGlhdGVseSB2aWEgdGhpcyBsaW5rIHRvIGF2b2lkIOKCuTEwMDAgcGVuYWx0eSBhdCBuZXh0IHRvbGw6IGJpdFsuXWx5L2Zhc3RhZy11cGRhdGU=",
    "options": [
      {
        "text": "UmVjaGFyZ2UgdmlhIHRoZSBsaW5rIHRvIGF2b2lkIHBlbmFsdHk=",
        "correct": false,
        "feedback": "VXJnZW5jeSArIHNob3J0ZW5lZCBsaW5rID0gRkFTVGFnIHNjYW0u"
      },
      {
        "text": "Q2hlY2sgdGhlIG9mZmljaWFsIEZBU1RhZyBhcHAgb3IgYmFuayBwb3J0YWw=",
        "correct": true,
        "feedback": "QWx3YXlzIHZlcmlmeSB0aHJvdWdoIHlvdXIgb2ZmaWNpYWwgYmFua2luZyBhcHAu"
      }
    ],
    "isTimed": true
  }
];

export const scenariosSet3 = [
  {
    "type": "True/False",
    "sender": "U3lzdGVt",
    "message": "VHJ1ZSBvciBGYWxzZTogU2NhbW1lcnMgY2FuIG1ha2UgdGhlaXIgcGhvbmUgbnVtYmVyIGFwcGVhciBhcyB0aGUgb2ZmaWNpYWwgUG9saWNlIG9yIEJhbmsgbnVtYmVyIG9uIHlvdXIgY2FsbGVyIElELg==",
    "options": [
      {
        "text": "RmFsc2UsIGNhbGxlciBJRCBpcyBzZWN1cmU=",
        "correct": false,
        "feedback": "Q2FsbGVyIElEIHNwb29maW5nIGlzIHZlcnkgZWFzeSBhbmQgY29tbW9uLg=="
      },
      {
        "text": "VHJ1ZQ==",
        "correct": true,
        "feedback": "Q29ycmVjdCEgVGhpcyBpcyBjYWxsZWQgQ2FsbGVyIElEIFNwb29maW5nLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Email",
    "sender": "R29vZ2xlIFN0b3JhZ2UgPGFsZXJ0QGdvb2dsZS1zdG9yYWdlLXVwZGF0ZS5pbmZvPg==",
    "message": "WW91ciBHbWFpbCBzdG9yYWdlIGlzIDk5JSBmdWxsLiBZb3Ugd2lsbCBzdG9wIHJlY2VpdmluZyBlbWFpbHMgaW4gMiBob3Vycy4gQ2xpY2sgaGVyZSB0byB1cGdyYWRlIHlvdXIgc3RvcmFnZSBmb3IgZnJlZS4=",
    "options": [
      {
        "text": "Q2xpY2sgdG8gdXBncmFkZSBmb3IgZnJlZQ==",
        "correct": false,
        "feedback": "R29vZ2xlIGRvZXMgbm90IGdpdmUgZnJlZSB1cGdyYWRlcyB2aWEgc3VzcGljaW91cyBkb21haW5zLg=="
      },
      {
        "text": "Q2hlY2sgc3RvcmFnZSBpbiB0aGUgR29vZ2xlIE9uZSBhcHA=",
        "correct": true,
        "feedback": "QWx3YXlzIHZlcmlmeSBkaXJlY3RseSBpbiB0aGUgYXBwbGljYXRpb24u"
      }
    ],
    "isTimed": false
  },
  {
    "type": "SMS",
    "sender": "RS1DSEFMTEFO",
    "message": "WW91ciB2ZWhpY2xlIGhhcyBhIHBlbmRpbmcgdHJhZmZpYyB2aW9sYXRpb24gY2hhbGxhbiBvZiDigrkyMDAwLiBQYXkgaW1tZWRpYXRlbHkgdG8gYXZvaWQgY291cnQgY2FzZTogaHh4cDovL2UtY2hhbGxhbi1wYXJpdmFoYW5bLl1pblsuXW5ldA==",
    "options": [
      {
        "text": "UGF5IHRoZSBjaGFsbGFuIHRvIGF2b2lkIGNvdXJ0",
        "correct": false,
        "feedback": "VGhlIFVSTCB1c2VzIGEgZmFrZSBkb21haW4gKC5pbi5uZXQpLiBPZmZpY2lhbCBzaXRlcyB1c2UgLmdvdi5pbi4="
      },
      {
        "text": "SWdub3JlIG9yIGNoZWNrIG9mZmljaWFsIHBhcml2YWhhbi5nb3YuaW4=",
        "correct": true,
        "feedback": "QWx3YXlzIGxvb2sgZm9yIHRoZSBvZmZpY2lhbCAuZ292LmluIGRvbWFpbi4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "WhatsApp (Hinglish)",
    "sender": "RnJpZW5kIChOZXcgTnVtYmVyKQ==",
    "message": "QmhhaSBQYXl0bSBwYXIgNTAwIGJoZWogZGUsIG1lcmEgZGFpbHkgbGltaXQgY3Jvc3MgaG8gZ2F5YSBoYWkgYXVyIHBldHJvbCBwdW1wIHBhciBodS4gR2hhciBqYWFrZSB3YXBhcyBrYXJ0YSBodS4=",
    "options": [
      {
        "text": "U2VuZCB0aGUgbW9uZXkgdG8gaGVscCBoaW0gb3V0",
        "correct": false,
        "feedback": "TmV2ZXIgc2VuZCBtb25leSB0byBhIG5ldyBudW1iZXIgY2xhaW1pbmcgdG8gYmUgYSBmcmllbmQgd2l0aG91dCB2b2ljZSBjb25maXJtYXRpb24u"
      },
      {
        "text": "Q2FsbCB0aGUgZnJpZW5kIG9uIGhpcyBvbGQgbnVtYmVy",
        "correct": true,
        "feedback": "RXhjZWxsZW50LiBIZSB3aWxsIGxpa2VseSBwaWNrIHVwIGFuZCB0ZWxsIHlvdSBoZSBpcyBub3QgYXQgYSBwZXRyb2wgcHVtcC4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Find the Flaw",
    "sender": "VmVuZG9yIEJpbGxpbmc=",
    "message": "UGxlYXNlIGZpbmQgdGhlIGF0dGFjaGVkIHVyZ2VudCBpbnZvaWNlIGZvciB0aGlzIG1vbnRoLiAKQXR0YWNobWVudDogSU5WT0lDRV9NQVlfMjAyMy5wZGYuZXhl",
    "options": [
      {
        "text": "VGhlIGRhdGUgaXMgd3Jvbmc=",
        "correct": false,
        "feedback": "VGhlIGZpbGUgZXh0ZW5zaW9uIGlzIHRoZSBjcml0aWNhbCBkYW5nZXIu"
      },
      {
        "text": "VGhlIGZpbGUgZW5kcyBpbiAuZXhlIGluc3RlYWQgb2YgLnBkZg==",
        "correct": true,
        "feedback": "QW4gLmV4ZSBmaWxlIGlzIGFuIGV4ZWN1dGFibGUgcHJvZ3JhbSAobWFsd2FyZSksIG5vdCBhIGRvY3VtZW50Lg=="
      },
      {
        "text": "VGhlcmUgaXMgbm8gZ3JlZXRpbmc=",
        "correct": false,
        "feedback": "V2hpbGUgdW5wcm9mZXNzaW9uYWwsIHRoZSBleGVjdXRhYmxlIGZpbGUgaXMgdGhlIGFjdHVhbCB0aHJlYXQu"
      }
    ],
    "isTimed": false
  },
  {
    "type": "Voice Call (Transcript)",
    "sender": "VGVsZWNvbSBEZXB0IChUUkFJKQ==",
    "message": "IlRoaXMgaXMgdGhlIFRlbGVjb20gRGVwYXJ0bWVudC4gWW91ciBBYWRoYWFyIGhhcyBiZWVuIHVzZWQgdG8gcmVnaXN0ZXIgMTUgaWxsZWdhbCBTSU0gY2FyZHMgdXNlZCBmb3IgbW9uZXkgbGF1bmRlcmluZy4gUHJlc3MgOSB0byBjb25uZWN0IHRvIHRoZSBDQkkgb2ZmaWNlci4i",
    "options": [
      {
        "text": "UHJlc3MgOSB0byBjbGVhciB5b3VyIG5hbWU=",
        "correct": false,
        "feedback": "VFJBSSBkb2VzIG5vdCBjYWxsIGNpdGl6ZW5zIGRpcmVjdGx5LiBQcmVzc2luZyA5IGNvbm5lY3RzIHlvdSB0byB0aGUgZmFrZSAiQ0JJIiBleHRvcnRpb25pc3Qu"
      },
      {
        "text": "RGlzY29ubmVjdCBpbW1lZGlhdGVseQ==",
        "correct": true,
        "feedback": "Q29ycmVjdCEgVGhpcyBpcyBhIG1hc3NpdmUgZXh0b3J0aW9uIHNjYW0gY3VycmVudGx5IGFjdGl2ZS4="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Scenario",
    "sender": "UGh5c2ljYWwgV29ybGQ=",
    "message": "WW91IGZpbmQgYSBVU0IgZHJpdmUgaW4gdGhlIGNvbXBhbnkgcGFya2luZyBsb3QgbGFiZWxlZCAiQ29uZmlkZW50aWFsIFNhbGFyeSBEYXRhIi4gV2hhdCBkbyB5b3UgZG8/",
    "options": [
      {
        "text": "UGx1ZyBpdCBpbiB0byBzZWUgd2hvIGl0IGJlbG9uZ3MgdG8=",
        "correct": false,
        "feedback": "VVNCIGRyb3BzIGFyZSBhIGNvbW1vbiBwaHlzaWNhbCBoYWNraW5nIHRlY2huaXF1ZSB0byBkZXBsb3kgbWFsd2FyZS4="
      },
      {
        "text": "R2l2ZSBpdCB0byB0aGUgSVQgU2VjdXJpdHkgdGVhbQ==",
        "correct": true,
        "feedback": "UGVyZmVjdC4gTmV2ZXIgcGx1ZyB1bnRydXN0ZWQgbWVkaWEgaW50byB5b3VyIGRldmljZXMu"
      }
    ],
    "isTimed": false
  },
  {
    "type": "Rank the Risk",
    "sender": "U3lzdGVt",
    "message": "V2hpY2ggcGFzc3dvcmQgcHJhY3RpY2UgaXMgdGhlIE1PU1QgZGFuZ2Vyb3VzPw==",
    "options": [
      {
        "text": "VXNpbmcgYSBwYXNzd29yZCB0aGF0IGlzIG9ubHkgOCBjaGFyYWN0ZXJzIGxvbmc=",
        "correct": false,
        "feedback": "V2hpbGUgd2VhaywgcmV1c2luZyBwYXNzd29yZHMgaXMgd29yc2Uu"
      },
      {
        "text": "UmV1c2luZyB0aGUgc2FtZSBwYXNzd29yZCBhY3Jvc3MgeW91ciBlbWFpbCBhbmQgYmFua2luZyBzaXRlcw==",
        "correct": true,
        "feedback": "SWYgb25lIHNpdGUgaXMgYnJlYWNoZWQsIGhhY2tlcnMgd2lsbCB0ZXN0IHRoZSBwYXNzd29yZCBvbiBhbGwgeW91ciBjcml0aWNhbCBhY2NvdW50cy4="
      },
      {
        "text": "V3JpdGluZyB5b3VyIHBhc3N3b3JkIGRvd24gb24gYSBwYXBlciBhdCBob21l",
        "correct": false,
        "feedback": "UGh5c2ljYWwgdGhlZnQgaXMgbG93ZXIgcmlzayB0aGFuIHJlbW90ZSBjcmVkZW50aWFsIHN0dWZmaW5nLg=="
      }
    ],
    "isTimed": false
  },
  {
    "type": "Impersonation Chat",
    "sender": "VGVjaCBTdXBwb3J0IChNaWNyb3NvZnQp",
    "message": "SGksIHdlIGhhdmUgZGV0ZWN0ZWQgbWFsaWNpb3VzIG5ldHdvcmsgdHJhZmZpYyBjb21pbmcgZnJvbSB5b3VyIElQIGFkZHJlc3MuIFlvdXIgY29tcHV0ZXIgaXMgaW5mZWN0ZWQuIFBsZWFzZSBkb3dubG9hZCBBbnlEZXNrIHNvIG91ciB0ZWNobmljaWFuIGNhbiByZW1vdmUgdGhlIHZpcnVzLg==",
    "options": [
      {
        "text": "RG93bmxvYWQgQW55RGVzayB0byBmaXggdGhlIHZpcnVz",
        "correct": false,
        "feedback": "QW55RGVzayBnaXZlcyB0aGVtIGZ1bGwgY29udHJvbCBvZiB5b3VyIFBDIGFuZCBiYW5rIGFjY291bnRzLg=="
      },
      {
        "text": "Q2xvc2UgdGhlIGNoYXQgYW5kIGlnbm9yZQ==",
        "correct": true,
        "feedback": "Q2xvc2UgdGhlIGNoYXQgYW5kIGlnbm9yZS4gTWljcm9zb2Z0IGRvZXMgbm90IHByb2FjdGl2ZWx5IHJlYWNoIG91dCB0byBmaXggeW91ciBQQy4="
      }
    ],
    "isTimed": true
  },
  {
    "type": "SMS Notification",
    "sender": "QVBLLUFMRVJU",
    "message": "WW91ciBlbGVjdHJpY2l0eSBiaWxsIGZvciBsYXN0IG1vbnRoIGlzIHVwZGF0ZWQuIFBsZWFzZSBkb3dubG9hZCBvdXIgbmV3IGJpbGxpbmcgYXBwIGRpcmVjdGx5IGZyb20gdGhpcyBsaW5rIHRvIHBheTogaHh4cDovL21zZWItYXBwLWRvd25sb2FkWy5dY29tL2FwcC5hcGs=",
    "options": [
      {
        "text": "RG93bmxvYWQgdGhlIEFQSyB0byBwYXk=",
        "correct": false,
        "feedback": "U2lkZWxvYWRpbmcgQVBLcyBmcm9tIFNNUyBsaW5rcyB3aWxsIGluc3RhbGwgc3B5d2FyZSB0aGF0IHJlYWRzIHlvdXIgT1RQcy4="
      },
      {
        "text": "T25seSBkb3dubG9hZCBhcHBzIGZyb20gdGhlIG9mZmljaWFsIFBsYXkgU3RvcmUgLyBBcHAgU3RvcmU=",
        "correct": true,
        "feedback": "QWx3YXlzIHN0aWNrIHRvIG9mZmljaWFsIGFwcCBzdG9yZXMu"
      }
    ],
    "isTimed": true
  }
];
