# Voice Phishing (Vishing) Detection System
## Detailed Technical Implementation Plan

**Project:** Aegis AI - Voice Phishing Detection Module  
**Component:** Phase 2 Enhancement  
**Scope:** Real-time vishing detection in Indian languages (Hindi, Marathi, Hinglish)  
**Timeline:** 4-6 weeks  
**Difficulty:** Advanced (ML + Audio Processing + NLP)

---

## 1. SYSTEM ARCHITECTURE OVERVIEW

### **A. High-Level Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    USER DEVICE (Android/iOS)                │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐   │
│  │  CallScreen Module                                   │   │
│  │  ├─ Intercept incoming call                         │   │
│  │  ├─ Optional recording (with consent)               │   │
│  │  ├─ Real-time audio stream processing               │   │
│  │  └─ Display risk warning if detected               │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Vishing Detection Pipeline (Local + Cloud)          │   │
│  │  ├─ Audio Capture (PCM/WAV format)                  │   │
│  │  ├─ Speech-to-Text (ASR - Google/Azure)            │   │
│  │  ├─ Language Detection (Hindi/Marathi/Hinglish)    │   │
│  │  └─ Transcript Analysis                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓                                    │
└─────────────────────────────────────────────────────────────┘
                           ↓
        ┌──────────────────────────────────────────┐
        │  Cloud Backend (Flask API)               │
        ├──────────────────────────────────────────┤
        │  ┌──────────────────────────────────────┐│
        │  │ Vishing Classifier                   ││
        │  │ ├─ Language Models (mBERT)           ││
        │  │ ├─ Feature Extraction                ││
        │  │ └─ Risk Scoring                      ││
        │  └──────────────────────────────────────┘│
        │                                           │
        │  ┌──────────────────────────────────────┐│
        │  │ Threat Intelligence DB               ││
        │  │ ├─ Known scammer phrases             ││
        │  │ ├─ Caller ID spoofing database       ││
        │  │ └─ Threat signatures                 ││
        │  └──────────────────────────────────────┘│
        │                                           │
        │  ┌──────────────────────────────────────┐│
        │  │ Analytics & Logging                  ││
        │  │ ├─ Detections per region             ││
        │  │ ├─ Model accuracy metrics            ││
        │  │ └─ User feedback loop                ││
        │  └──────────────────────────────────────┘│
        └──────────────────────────────────────────┘
                        ↓
        ┌──────────────────────────────────────────┐
        │  User Notification                       │
        │  ├─ Toast notification (low priority)   │
        │  ├─ Call drop suggestion (high priority)│
        │  ├─ Report to authorities button        │
        │  └─ Gamified training link              │
        └──────────────────────────────────────────┘
```

---

## 2. COMPONENT 1: AUDIO CAPTURE & PROCESSING

### **A. Android Implementation (Primary)**

#### **2.1.1 Permissions Required**

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

#### **2.1.2 Call Interception Code**

```java
// VishingCallReceiver.java - Detects incoming calls

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.content.SharedPreferences;

public class VishingCallReceiver extends BroadcastReceiver {
    
    private static String lastPhoneNumber = "";
    private static final String PREF_NAME = "vishing_prefs";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(
                TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            // Step 1: Call is ringing
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                lastPhoneNumber = incomingNumber;
                
                // Check if number is spoofed/suspicious
                checkCallerID(context, incomingNumber);
                
                // Start recording with user permission check
                if (isRecordingEnabled(context)) {
                    startCallRecording(context, incomingNumber);
                }
            }
            
            // Step 2: Call is active
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                // Call is ongoing
                // Start real-time speech recognition
                startRealTimeSpeechToText(context);
            }
            
            // Step 3: Call ended
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Stop recording and processing
                stopCallRecording();
                // Analyze complete transcript
                analyzeCallTranscript(context, lastPhoneNumber);
            }
        }
    }
    
    private void checkCallerID(Context context, String number) {
        // Check against known spoofed caller IDs database
        // Use TrueCaller API or similar
        
        if (isLikelySpoofed(number)) {
            showWarning(context, "Caller ID may be spoofed");
        }
    }
    
    private void startCallRecording(Context context, String number) {
        Intent recordingIntent = new Intent(context, CallRecorderService.class);
        recordingIntent.putExtra("phone_number", number);
        recordingIntent.putExtra("timestamp", System.currentTimeMillis());
        context.startService(recordingIntent);
    }
    
    private void startRealTimeSpeechToText(Context context) {
        Intent sttIntent = new Intent(context, RealTimeSpeechToTextService.class);
        context.startService(sttIntent);
    }
}
```

#### **2.1.3 Audio Recording Service**

```java
// CallRecorderService.java

public class CallRecorderService extends Service {
    
    private MediaRecorder mediaRecorder;
    private String outputPath;
    private String phoneNumber;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        phoneNumber = intent.getStringExtra("phone_number");
        long timestamp = intent.getLongExtra("timestamp", 0);
        
        // Create output file path
        String fileName = "call_" + phoneNumber + "_" + timestamp + ".wav";
        outputPath = getFilesDir().getAbsolutePath() + "/" + fileName;
        
        // Initialize MediaRecorder
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioChannels(1); // Mono
            mediaRecorder.setAudioSamplingRate(16000); // 16 kHz for speech recognition
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(outputPath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            Log.d("CallRecorder", "Recording started: " + outputPath);
            
        } catch (IOException e) {
            Log.e("CallRecorder", "Failed to start recording", e);
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            
            // Upload recording to server for analysis
            uploadRecordingForAnalysis(outputPath, phoneNumber);
        }
        super.onDestroy();
    }
    
    private void uploadRecordingForAnalysis(String filePath, String phoneNumber) {
        File file = new File(filePath);
        
        // Create multipart request to upload audio
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", file.getName(),
                RequestBody.create(file, MediaType.parse("audio/wav")))
            .addFormDataPart("phone_number", phoneNumber)
            .build();
        
        Request request = new Request.Builder()
            .url("https://api.aegis.example.com/analyze_call")
            .post(requestBody)
            .build();
        
        // Send via OkHttp
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                // Handle server response with vishing detection result
                handleVishingDetectionResult(response);
            }
            
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UploadRecording", "Failed to upload", e);
            }
        });
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
```

---

## 3. COMPONENT 2: SPEECH-TO-TEXT CONVERSION

### **A. Real-Time Speech Recognition**

#### **3.1 Google Cloud Speech-to-Text API Setup**

```python
# Python backend - Initialize Google STT

from google.cloud import speech_v1
from google.api_core.gapic_v1 import client_info

client = speech_v1.SpeechClient()

config = speech_v1.RecognitionConfig(
    encoding=speech_v1.RecognitionConfig.AudioEncoding.LINEAR16,
    sample_rate_hertz=16000,
    language_code="hi-IN",  # Hindi - India
    enable_automatic_punctuation=True,
    
    # Enhanced speech recognition for accuracy
    speech_contexts=[
        speech_v1.SpeechContext(
            phrases=[
                "UPI", "block", "verify", "account", "immediate",
                "OTP", "password", "bank", "Aadhar", "KYC",
                # Scam-specific vocabulary
                "तुरंत", "अभी", "ब्लॉक", "खाता", "सत्यापन"
            ],
            boost=20.0  # Boost recognition of scam keywords
        )
    ],
    
    # Support multiple languages
    alternative_language_codes=["mr-IN", "en-IN", "ta-IN"]  # Marathi, English, Tamil
)

# Streaming recognition for real-time processing
streaming_config = speech_v1.StreamingRecognitionConfig(
    config=config,
    interim_results=True  # Get partial results before final
)
```

#### **3.2 Real-Time Transcript Processing**

```python
# Flask endpoint to process real-time audio stream

from flask import Flask, request, jsonify
from google.cloud import speech_v1
import threading

app = Flask(__name__)

@app.route('/process_call_audio', methods=['POST'])
def process_call_audio():
    """
    Receives audio chunks from mobile app in real-time,
    converts to text, and analyzes for vishing patterns
    """
    
    audio_data = request.files['audio'].read()
    phone_number = request.form.get('phone_number')
    timestamp = request.form.get('timestamp')
    
    # Detect language automatically
    language = detect_language(audio_data)  # Returns 'hi', 'mr', 'hinglish', etc.
    
    # Convert audio to text
    client = speech_v1.SpeechClient()
    
    audio = speech_v1.RecognitionAudio(content=audio_data)
    
    config = speech_v1.RecognitionConfig(
        encoding=speech_v1.RecognitionConfig.AudioEncoding.LINEAR16,
        sample_rate_hertz=16000,
        language_code=map_language_to_code(language),
        enable_automatic_punctuation=True,
    )
    
    response = client.recognize(config=config, audio=audio)
    
    # Extract transcript from response
    transcript = ""
    confidence_scores = []
    
    for result in response.results:
        transcript += result.alternatives[0].transcript
        confidence_scores.append(result.alternatives[0].confidence)
    
    # Analyze transcript for vishing patterns
    vishing_score = analyze_transcript_for_vishing(
        transcript=transcript,
        language=language,
        phone_number=phone_number
    )
    
    # Return real-time detection result
    return jsonify({
        "transcript": transcript,
        "language": language,
        "vishing_probability": vishing_score,
        "confidence": sum(confidence_scores) / len(confidence_scores) if confidence_scores else 0,
        "recommendation": "BLOCK" if vishing_score > 0.7 else "WARN" if vishing_score > 0.5 else "SAFE"
    })


def detect_language(audio_data):
    """Detect language from audio using Google Cloud"""
    
    client = speech_v1.SpeechClient()
    audio = speech_v1.RecognitionAudio(content=audio_data)
    
    # Try recognition in multiple languages
    languages = ['hi-IN', 'mr-IN', 'ta-IN', 'te-IN', 'en-IN']
    results = {}
    
    for lang_code in languages:
        config = speech_v1.RecognitionConfig(
            encoding=speech_v1.RecognitionConfig.AudioEncoding.LINEAR16,
            sample_rate_hertz=16000,
            language_code=lang_code,
        )
        
        response = client.recognize(config=config, audio=audio)
        
        # Confidence is higher if it's the correct language
        confidence = 0
        if response.results:
            confidence = response.results[0].alternatives[0].confidence
        results[lang_code] = confidence
    
    # Return language with highest confidence
    best_lang = max(results, key=results.get)
    return map_code_to_language(best_lang)  # Returns 'hindi', 'marathi', etc.

```

---

## 4. COMPONENT 3: VISHING PATTERN DETECTION

### **A. Feature Extraction from Transcripts**

```python
# vishing_classifier.py

import re
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer

class VishingFeatureExtractor:
    
    def __init__(self, language='hindi'):
        self.language = language
        self.urgency_patterns = self._load_urgency_patterns()
        self.threat_patterns = self._load_threat_patterns()
        self.impersonation_patterns = self._load_impersonation_patterns()
    
    def _load_urgency_patterns(self):
        """Language-specific urgency/pressure keywords"""
        
        patterns = {
            'hindi': {
                'immediate': ['तुरंत', 'अभी', 'तुरंत ही', 'तुरंत करो', 'जल्दी'],
                'urgent_action': ['करना होगा', 'करना जरूरी है', 'नहीं तो'],
                'time_limited': ['अभी', 'तुरंत', 'आज', 'रात को', '1 घंटे में'],
                'consequence': ['खाता बंद', 'पैसे जब्त', 'समस्या', 'मुसीबत'],
            },
            'marathi': {
                'immediate': ['लगेच', 'तुरंत', 'आता', 'लगेच करा'],
                'urgent_action': ['करावे लागेल', 'करण्यास हवे'],
                'time_limited': ['लगेच', 'आज', 'रात्री'],
                'consequence': ['खाते बंद', 'जाळवून टाकेल', 'अडचण'],
            },
            'english': {
                'immediate': ['immediately', 'urgent', 'asap', 'right now'],
                'urgent_action': ['must', 'need to', 'have to'],
                'time_limited': ['today', 'within hours', 'before midnight'],
                'consequence': ['block', 'suspend', 'freeze', 'close'],
            }
        }
        
        return patterns.get(self.language, patterns['english'])
    
    def _load_threat_patterns(self):
        """Words indicating threats/problems"""
        
        threats = {
            'hindi': {
                'account_threat': ['खाता', 'अकाउंट', 'account', 'ब्लॉक', 'suspend'],
                'security_threat': ['असुरक्षित', 'हैक', 'अनाधिकृत', 'unauthorized'],
                'financial_threat': ['लेनदेन', 'राशि', 'पैसे', 'transaction'],
                'action_threat': ['तुरंत करना', 'अभी करना', 'करना होगा'],
            },
            'marathi': {
                'account_threat': ['खाते', 'account', 'अकाउंट', 'बंद'],
                'security_threat': ['असुरक्षित', 'हैक', 'अनाधिकृत'],
                'financial_threat': ['व्यवहार', 'रक्कम', 'पैसे'],
                'action_threat': ['करावे', 'करण्यास', 'लगेच'],
            }
        }
        
        return threats.get(self.language, {})
    
    def _load_impersonation_patterns(self):
        """Patterns indicating impersonation"""
        
        entities = {
            'banks': ['ICICI', 'HDFC', 'SBI', 'Axis', 'Kotak', 'बैंक', 'बँक'],
            'upi_apps': ['Google Pay', 'PhonePe', 'Paytm', 'WhatsApp Pay', 'पेटीएम'],
            'government': ['Aadhar', 'आधार', 'ITR', 'income tax', 'आयकर'],
            'companies': ['Amazon', 'Flipkart', 'Zomato', 'Uber'],
            'authority_words': ['verify', 'confirm', 'validate', 'सत्यापित', 'पुष्टि'],
        }
        
        return entities
    
    def extract_features(self, transcript):
        """Extract numerical features from transcript"""
        
        features = {}
        
        # 1. Length features
        features['transcript_length'] = len(transcript)
        features['word_count'] = len(transcript.split())
        features['sentence_count'] = len(re.split(r'[।.!?]', transcript))
        
        # 2. Urgency pattern matches
        urgency_count = 0
        for category, words in self.urgency_patterns.items():
            for word in words:
                if word.lower() in transcript.lower():
                    urgency_count += 1
        features['urgency_pattern_count'] = urgency_count
        features['urgency_score'] = min(urgency_count / features['word_count'], 1.0)
        
        # 3. Threat pattern matches
        threat_count = 0
        for category, words in self.threat_patterns.items():
            for word in words:
                if word.lower() in transcript.lower():
                    threat_count += 1
        features['threat_pattern_count'] = threat_count
        
        # 4. Impersonation detection
        impersonation_score = 0
        for entity_type, entities in self.impersonation_patterns.items():
            if entity_type != 'authority_words':
                for entity in entities:
                    if entity.lower() in transcript.lower():
                        impersonation_score += 1
        features['impersonation_score'] = impersonation_score
        
        # 5. Authority language
        authority_words = self.impersonation_patterns.get('authority_words', [])
        authority_count = sum(1 for word in authority_words 
                            if word.lower() in transcript.lower())
        features['authority_language_count'] = authority_count
        
        # 6. Credential request detection
        credential_keywords = ['password', 'OTP', 'PIN', 'CVV', 'account', 
                            'पासवर्ड', 'ओटीपी', 'खाता']
        credential_count = sum(1 for keyword in credential_keywords 
                             if keyword.lower() in transcript.lower())
        features['credential_request_score'] = credential_count
        
        # 7. Emotional manipulation detection
        emotional_words = ['worried', 'dangerous', 'risky', 'problem', 'trouble',
                         'चिंतित', 'खतरनाक', 'समस्या', 'परेशानी']
        emotional_count = sum(1 for word in emotional_words 
                            if word.lower() in transcript.lower())
        features['emotional_manipulation_score'] = emotional_count
        
        # 8. Question vs Statement ratio (scammers ask fewer questions)
        questions = transcript.count('?')
        statements = features['sentence_count'] - questions
        features['question_ratio'] = questions / features['sentence_count'] if features['sentence_count'] > 0 else 0
        
        # 9. Refusal handling (legitimate calls respect "no", scam calls don't)
        refusal_words = ['नहीं', 'no', 'नहीं चाहता', 'don\'t want']
        refusal_count = sum(1 for word in refusal_words 
                          if word.lower() in transcript.lower())
        features['refusal_handling_score'] = refusal_count
        
        return features

```

### **B. Vishing Classification Model**

```python
# vishing_classifier_model.py

import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
import numpy as np

class VishingClassifier:
    
    def __init__(self, language='hindi'):
        self.language = language
        self.model = None
        self.scaler = StandardScaler()
        self.feature_extractor = VishingFeatureExtractor(language)
        self.load_trained_model()
    
    def load_trained_model(self):
        """Load pre-trained vishing detection model"""
        
        model_path = f"models/vishing_classifier_{self.language}.pkl"
        try:
            self.model = joblib.load(model_path)
            print(f"Loaded vishing model for {self.language}")
        except FileNotFoundError:
            print(f"Model not found. Will train new model.")
            self.model = None
    
    def train_model(self, training_transcripts, training_labels):
        """
        Train vishing detection model on synthetic call transcripts
        
        Args:
            training_transcripts: List of call transcripts
            training_labels: List of binary labels (1=vishing, 0=legitimate)
        """
        
        # Feature extraction
        X = []
        for transcript in training_transcripts:
            features = self.feature_extractor.extract_features(transcript)
            feature_vector = self._features_dict_to_vector(features)
            X.append(feature_vector)
        
        X = np.array(X)
        y = np.array(training_labels)
        
        # Scale features
        X_scaled = self.scaler.fit_transform(X)
        
        # Train Random Forest
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=15,
            min_samples_split=5,
            random_state=42,
            class_weight='balanced'  # Handle class imbalance
        )
        
        self.model.fit(X_scaled, y)
        
        # Save model
        joblib.dump(self.model, f"models/vishing_classifier_{self.language}.pkl")
        joblib.dump(self.scaler, f"models/scaler_{self.language}.pkl")
        
        print(f"Vishing model trained and saved for {self.language}")
    
    def predict(self, transcript):
        """
        Predict if transcript is vishing
        
        Returns:
            {
                'is_vishing': bool,
                'probability': float (0-1),
                'risk_level': 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
                'reasoning': dict of feature importance
            }
        """
        
        if self.model is None:
            return {'is_vishing': False, 'probability': 0, 'risk_level': 'UNKNOWN'}
        
        # Extract features
        features = self.feature_extractor.extract_features(transcript)
        feature_vector = self._features_dict_to_vector(features)
        feature_vector = self.scaler.transform([feature_vector])[0]
        
        # Get prediction and probability
        prediction = self.model.predict([feature_vector])[0]
        probability = self.model.predict_proba([feature_vector])[0][1]
        
        # Determine risk level
        if probability > 0.8:
            risk_level = 'CRITICAL'
        elif probability > 0.6:
            risk_level = 'HIGH'
        elif probability > 0.4:
            risk_level = 'MEDIUM'
        else:
            risk_level = 'LOW'
        
        # Get feature importance
        feature_importance = self._get_feature_importance(feature_vector)
        
        return {
            'is_vishing': prediction == 1,
            'probability': float(probability),
            'risk_level': risk_level,
            'confidence': float(probability),
            'reasoning': feature_importance,
            'features': features
        }
    
    def _features_dict_to_vector(self, features_dict):
        """Convert feature dictionary to vector for model"""
        
        feature_order = [
            'transcript_length', 'word_count', 'sentence_count',
            'urgency_score', 'threat_pattern_count',
            'impersonation_score', 'authority_language_count',
            'credential_request_score', 'emotional_manipulation_score',
            'question_ratio', 'refusal_handling_score'
        ]
        
        return np.array([features_dict.get(f, 0) for f in feature_order])
    
    def _get_feature_importance(self, feature_vector):
        """Explain which features contributed to detection"""
        
        if self.model is None:
            return {}
        
        # Get feature importances from model
        importances = self.model.feature_importances_
        feature_names = [
            'transcript_length', 'word_count', 'sentence_count',
            'urgency_score', 'threat_pattern_count',
            'impersonation_score', 'authority_language_count',
            'credential_request_score', 'emotional_manipulation_score',
            'question_ratio', 'refusal_handling_score'
        ]
        
        # Return top 5 most important features
        top_indices = np.argsort(importances)[-5:][::-1]
        
        reasoning = {}
        for idx in top_indices:
            reasoning[feature_names[idx]] = float(importances[idx])
        
        return reasoning

```

---

## 5. GENERATING SYNTHETIC VISHING DATASET

### **A. Dataset Creation**

```python
# synthetic_vishing_dataset_generator.py

import random
from datetime import datetime

class SyntheticVishingDatasetGenerator:
    
    def __init__(self, language='hindi'):
        self.language = language
        self.scam_scripts = self._load_scam_scripts()
    
    def _load_scam_scripts(self):
        """Load templates for common vishing attacks"""
        
        scripts = {
            'hindi': {
                'upi_fraud': [
                    "नमस्ते, यह {bank_name} बोल रहे हैं। आपके UPI अकाउंट में असामान्य गतिविधि देखी गई है। तुरंत अपना खाता सत्यापित करें।",
                    "आपका {upi_app} अकाउंट ब्लॉक हो गया है। तुरंत अपडेट करें नहीं तो पैसे जब्त हो जाएंगे।",
                    "बताइए, आपका Google Pay खाता सुरक्षित है? हाल ही में असामान्य लेनदेन हुआ है।",
                ],
                'kyc_fraud': [
                    "यह आधार विभाग से बोल रहे हैं। आपका KYC अद्यतन करना आवश्यक है, नहीं तो आपका खाता बंद हो जाएगा।",
                    "आपके आधार की वैधता समाप्त हो रही है। तुरंत इस लिंक से अपडेट करें।",
                ],
                'bank_impersonation': [
                    "यह ICICI बैंक से हूँ। आपके खाते में अनाधिकृत प्रवेश का संदेह है। तुरंत अपना पासवर्ड बदलें।",
                    "आपके SBI खाते से ₹50,000 का लेनदेन किया गया। यदि यह आपने नहीं किया तो तुरंत OTP भेजें।",
                ],
                'job_fraud': [
                    "नमस्ते! आपको एक बेहतरीन नौकरी का अवसर मिला है। घर से काम करें, ₹50,000 महीना कमाएं।",
                    "आपने हमारी कंपनी में नौकरी के लिए आवेदन किया था। आप सफल हो गए! अपना विवरण भेजें।",
                ],
            },
            'marathi': {
                'upi_fraud': [
                    "नमस्ते, यह {bank_name} बोलत आहे. तुमच्या UPI खात्यात अनामधिक क्रिया दिसली. लगेच अकाउंट व्हेरिफाय करा.",
                    "तुमचा {upi_app} खाता बंद केला जाणार आहे. आता अद्यतन करा!",
                ],
                'kyc_fraud': [
                    "आधार विभागातून बोलतेय. तुमचे KYC अद्यतन करावे लागणार आहे.",
                ],
            },
            'hinglish': {
                'upi_fraud': [
                    "Hello, yeh {bank_name} bol rahe hain. Aapka UPI account block ho gaya. Turant verify karo!",
                    "Aapka Google Pay account limited ho gaya hai. Immediately update karo.",
                ],
                'job_fraud': [
                    "Hi! Work from home opportunity! Earn 50,000 per day. No experience needed. Interested?",
                ],
            }
        }
        
        return scripts.get(self.language, scripts['english'])
    
    def generate_dataset(self, n_samples=2000):
        """
        Generate synthetic vishing call transcripts
        
        Returns:
            List of (transcript, label) tuples
        """
        
        dataset = []
        
        # Scam call scripts
        scam_count = 0
        while scam_count < n_samples // 2:
            script_type = random.choice(list(self.scam_scripts.keys()))
            script_template = random.choice(self.scam_scripts[script_type])
            
            # Generate variations
            entities = {
                'bank_name': random.choice(['ICICI', 'HDFC', 'SBI', 'Axis']),
                'upi_app': random.choice(['Google Pay', 'PhonePe', 'Paytm']),
            }
            
            transcript = script_template.format(**entities)
            
            # Add natural variations (hesitation, repetition)
            transcript = self._add_speech_variations(transcript)
            
            dataset.append((transcript, 1))  # Label: 1 = vishing
            scam_count += 1
        
        # Legitimate call scripts (counterexamples)
        legitimate_count = 0
        legitimate_scripts = [
            "आप अपना हेल्थ इंश्योरेंस रिन्यू कराना चाहते हैं?",
            "This is from customer service. Is this a good time to talk?",
            "तुमचा ऑर्डर डिलीवर झाला आहे. कृपया सहमती दर्शा.",
            "Hello, calling about your subscription. Can I help?",
        ]
        
        while legitimate_count < n_samples // 2:
            script = random.choice(legitimate_scripts)
            transcript = self._add_speech_variations(script)
            dataset.append((transcript, 0))  # Label: 0 = legitimate
            legitimate_count += 1
        
        return dataset
    
    def _add_speech_variations(self, transcript):
        """Add natural speech patterns (hesitations, repetitions)"""
        
        variations = [
            lambda t: t,  # No variation
            lambda t: "हाँ, " + t if self.language == 'hindi' else t,  # Add "haan"
            lambda t: t + " कृपया।" if self.language == 'hindi' else t + " please.",  # Add politeness
            lambda t: t.replace("।", "। एक बार फिर से दोहराता हूँ -") if self.language == 'hindi' else t,
        ]
        
        return random.choice(variations)(transcript)

```

---

## 6. DEPLOYMENT & API ENDPOINTS

### **A. Flask API Server**

```python
# server.py - Main API for vishing detection

from flask import Flask, request, jsonify
from vishing_classifier_model import VishingClassifier
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Initialize classifiers for each language
classifiers = {
    'hindi': VishingClassifier('hindi'),
    'marathi': VishingClassifier('marathi'),
    'hinglish': VishingClassifier('hinglish'),
    'english': VishingClassifier('english'),
}

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'version': '1.0'})

@app.route('/detect_vishing', methods=['POST'])
def detect_vishing():
    """
    Main endpoint for vishing detection
    
    Request:
    {
        "transcript": "आपका UPI खाता ब्लॉक हो गया...",
        "language": "hindi",
        "phone_number": "+919876543210",
        "timestamp": 1620000000
    }
    
    Response:
    {
        "is_vishing": true,
        "probability": 0.92,
        "risk_level": "CRITICAL",
        "action": "BLOCK_CALL",
        "reasoning": {...}
    }
    """
    
    try:
        data = request.json
        
        transcript = data.get('transcript', '')
        language = data.get('language', 'hindi').lower()
        phone_number = data.get('phone_number', '')
        timestamp = data.get('timestamp', 0)
        
        # Validate input
        if not transcript or len(transcript) < 10:
            return jsonify({'error': 'Invalid transcript'}), 400
        
        # Get appropriate classifier
        if language not in classifiers:
            language = 'english'  # Fallback
        
        classifier = classifiers[language]
        
        # Predict
        result = classifier.predict(transcript)
        
        # Add metadata
        result['phone_number'] = phone_number
        result['timestamp'] = timestamp
        result['language'] = language
        
        # Determine action
        if result['probability'] > 0.8:
            result['action'] = 'BLOCK_CALL'
        elif result['probability'] > 0.6:
            result['action'] = 'WARN_USER'
        else:
            result['action'] = 'ALLOW'
        
        # Log detection
        logging.info(f"Vishing detection: {phone_number} - {result['risk_level']}")
        
        return jsonify(result)
    
    except Exception as e:
        logging.error(f"Error in vishing detection: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/train_model', methods=['POST'])
def train_model():
    """
    Train/update vishing model with new data
    
    Request:
    {
        "transcripts": [...],
        "labels": [...],
        "language": "hindi"
    }
    """
    
    try:
        data = request.json
        transcripts = data.get('transcripts', [])
        labels = data.get('labels', [])
        language = data.get('language', 'hindi')
        
        if len(transcripts) != len(labels):
            return jsonify({'error': 'Transcripts and labels length mismatch'}), 400
        
        classifier = classifiers[language]
        classifier.train_model(transcripts, labels)
        
        return jsonify({'status': 'Model trained successfully'})
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/feedback', methods=['POST'])
def log_feedback():
    """
    Log user feedback for model improvement
    
    Request:
    {
        "phone_number": "+919876543210",
        "transcript": "...",
        "actual_label": 1,  # 1=vishing, 0=legitimate
        "predicted_label": 1,
        "was_correct": true
    }
    """
    
    try:
        data = request.json
        
        # Log to database for analysis
        logging.info(f"Feedback: {data}")
        
        # Store for later retraining
        # ... database operations ...
        
        return jsonify({'status': 'Feedback recorded'})
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
```

---

## 7. INTEGRATION WITH AEGIS AI APP

### **A. User Notification Flow**

```java
// UserNotificationManager.java - Display vishing alerts to user

public class UserNotificationManager {
    
    private Context context;
    private static final String CHANNEL_ID = "vishing_alerts";
    
    public UserNotificationManager(Context context) {
        this.context = context;
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Vishing Alerts";
            String description = "Real-time voice phishing alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            
            NotificationManager manager = context.getSystemService(
                NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    public void showVishingAlert(String phoneNumber, double probability, 
                                String riskLevel) {
        
        // Determine action and message
        String title, message, action;
        int priority;
        
        if (probability > 0.8) {
            title = "⚠️ CRITICAL: Likely Voice Phishing";
            message = "This call appears to be a scam. Recommended: DROP CALL";
            action = "DROP";
            priority = NotificationCompat.PRIORITY_MAX;
        } else if (probability > 0.6) {
            title = "⚠️ WARNING: Suspicious Call";
            message = "Be cautious. Ask for official callback number.";
            action = "VERIFY";
            priority = NotificationCompat.PRIORITY_HIGH;
        } else {
            title = "ℹ️ Call Analysis";
            message = "Call appears safe. Stay alert.";
            action = "OK";
            priority = NotificationCompat.PRIORITY_DEFAULT;
        }
        
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(message + "\n\nCaller: " + phoneNumber))
            .addAction(R.drawable.ic_block, "Block", getBlockIntent())
            .addAction(R.drawable.ic_report, "Report", getReportIntent())
            .setVibrate(new long[]{0, 500, 200, 500})
            .setSound(RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION));
        
        NotificationManager manager = NotificationManagerCompat.from(context);
        manager.notify(phoneNumber.hashCode(), builder.build());
    }
    
    private PendingIntent getBlockIntent() {
        Intent intent = new Intent(context, VishingResponseActivity.class);
        intent.setAction("ACTION_BLOCK_CALL");
        return PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    private PendingIntent getReportIntent() {
        Intent intent = new Intent(context, VishingResponseActivity.class);
        intent.setAction("ACTION_REPORT_CALL");
        return PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
```

---

## 8. TESTING & EVALUATION

### **A. Testing Plan**

```
TEST PHASE 1: Unit Testing
├─ Test speech recognition accuracy
├─ Test feature extraction
├─ Test model predictions
└─ Target: >95% unit test pass rate

TEST PHASE 2: Integration Testing
├─ Test API endpoints
├─ Test mobile app integration
├─ Test database logging
└─ Target: All endpoints responding correctly

TEST PHASE 3: Accuracy Testing
├─ Test on golden dataset (manually verified scams/legitimate calls)
├─ Measure: Precision, Recall, F1-Score
├─ Target: >85% accuracy for each language

TEST PHASE 4: Performance Testing
├─ Measure API response time: <500ms
├─ Measure memory usage: <100MB on mobile
├─ Test under load: 1000 concurrent requests
└─ Target: All performance metrics met

TEST PHASE 5: User Testing
├─ Beta test with 100 users
├─ Measure false positive rate: <5%
├─ Collect user feedback
└─ Target: User satisfaction >4.5/5 stars
```

---

## 9. IMPLEMENTATION TIMELINE

```
WEEK 1-2: Setup & Audio Processing
├─ Set up Android recording infrastructure
├─ Integrate Google Cloud Speech-to-Text
├─ Test real-time audio capture
└─ Deliverable: Audio capture working

WEEK 2-3: Dataset & Training
├─ Generate synthetic vishing dataset (2,000 samples)
├─ Create annotation guidelines
├─ Train baseline models
└─ Deliverable: Trained models for 3 languages

WEEK 3-4: API Development
├─ Build Flask API endpoints
├─ Implement model serving
├─ Test API with mobile app
└─ Deliverable: Working API with documentation

WEEK 4-5: Mobile Integration
├─ Integrate detection into Aegis AI app
├─ Implement user notifications
├─ Build feedback mechanism
└─ Deliverable: Full mobile app integration

WEEK 5-6: Testing & Optimization
├─ Conduct accuracy testing
├─ Performance optimization
├─ User acceptance testing
└─ Deliverable: Production-ready system

WEEK 6: Deployment
├─ Deploy API to production
├─ Release updated mobile app
├─ Monitor real-world performance
└─ Deliverable: Live system
```

---

## 10. EXPECTED OUTCOMES

**Accuracy Targets:**
- Hindi: >90% accuracy
- Marathi: >88% accuracy
- Hinglish: >85% accuracy (code-switching is challenging)

**Performance Targets:**
- API response time: <500ms
- Mobile memory usage: <100MB
- False positive rate: <5%

**Patent Claims Supported:**
- Claim 2: "Voice phishing detection in Indian languages"
- Supporting claims for multilingual NLP and real-time detection

---

*Voice Phishing Detection Implementation Plan Complete*  
*Total implementation effort: 4-6 weeks*  
*Estimated complexity: Advanced*  
*Expected impact: High (novel contribution to patent portfolio)*
