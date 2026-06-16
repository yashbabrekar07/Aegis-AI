import { useState, useEffect, useRef } from 'react';
import { getRankFromXp } from './Simulate';
import { apiUrl } from '../lib/api';
import { getStoredUsername, getStoredEmail, getEmailLocalPart } from '../utils/userStorage';

export default function Dashboard() {
  const displayUsername = getStoredUsername();
  const emailLocal = getEmailLocalPart(getStoredEmail());
  const userEmail = getStoredEmail() || 'Not provided';
  
  // Load rank from training xp
  const xp = parseInt(localStorage.getItem('aegis_training_xp')) || 0;
  const history = JSON.parse(localStorage.getItem('aegis_training_history')) || [];
  const currentRank = getRankFromXp(xp);
  
  const hasHistory = history.length > 0;

  const [toggles, setToggles] = useState(() => {
    const saved = localStorage.getItem('aegis_smart_alerts');
    if (saved) {
      try { return JSON.parse(saved); } catch { /* ignore */ }
    }
    return { sms: true, calls: false, whatsapp: true };
  });

  const [profile, setProfile] = useState({ userId: 'Loading...', phone: '' });
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [chartLoaded, setChartLoaded] = useState(false);

  // Phone verification state machine: 'idle' | 'input' | 'otp'
  const [phoneStep, setPhoneStep] = useState('idle');
  const [phoneInput, setPhoneInput] = useState('');
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const [phoneStatus, setPhoneStatus] = useState(''); // loading message
  const [phoneError, setPhoneError] = useState('');
  const [resendCooldown, setResendCooldown] = useState(0);
  const otpRefs = useRef([]);
  const cooldownRef = useRef(null);

  useEffect(() => {
    const savedPhone = localStorage.getItem('aegis_user_phone') || '';
    const isVerified = localStorage.getItem('aegis_phone_verified') === 'true';
    const phoneParam = savedPhone ? `&phone=${encodeURIComponent(savedPhone)}` : '';
    
    fetch(apiUrl(`/api/user/profile?email=${encodeURIComponent(userEmail)}${phoneParam}`))
      .then(res => res.json())
      .then(data => {
        setProfile({ userId: data.user_id, phone: savedPhone });
        setPhoneInput(savedPhone);
        setPhoneVerified(isVerified && !!savedPhone);
      })
      .catch(() => setProfile({ userId: 'Error', phone: '' }));
      
    setTimeout(() => setChartLoaded(true), 100);
  }, [userEmail]);

  // Cleanup cooldown timer
  useEffect(() => {
    return () => { if (cooldownRef.current) clearInterval(cooldownRef.current); };
  }, []);

  const startCooldown = (seconds = 60) => {
    setResendCooldown(seconds);
    if (cooldownRef.current) clearInterval(cooldownRef.current);
    cooldownRef.current = setInterval(() => {
      setResendCooldown(prev => {
        if (prev <= 1) { clearInterval(cooldownRef.current); return 0; }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSendOtp = async () => {
    const phone = phoneInput.trim();
    if (phone.length < 10) {
      setPhoneError('Please enter a valid phone number (at least 10 digits)');
      return;
    }
    setPhoneError('');
    setPhoneStatus('Sending OTP...');
    try {
      const res = await fetch(apiUrl('/api/auth/send-phone-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, email: userEmail }),
      });
      const data = await res.json();
      if (data.ok) {
        setPhoneStep('otp');
        setOtpDigits(['', '', '', '', '', '']);
        setPhoneStatus('');
        startCooldown(60);
        // In dev mode the backend returns the OTP — show a hint
        if (data.dev_otp) {
          setPhoneError('');
          setPhoneStatus(`Dev mode — your OTP is: ${data.dev_otp}`);
        }
        // Focus first OTP box
        setTimeout(() => otpRefs.current[0]?.focus(), 100);
      } else {
        setPhoneError(data.error || 'Failed to send OTP');
        setPhoneStatus('');
      }
    } catch (err) {
      setPhoneError('Could not reach server. Is the backend running?');
      setPhoneStatus('');
    }
  };

  const handleVerifyOtp = async () => {
    const otp = otpDigits.join('');
    if (otp.length !== 6) {
      setPhoneError('Please enter the full 6-digit OTP');
      return;
    }
    const phone = phoneInput.trim();
    setPhoneError('');
    setPhoneStatus('Verifying...');
    try {
      const res = await fetch(apiUrl('/api/auth/verify-phone-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, otp, email: userEmail }),
      });
      const data = await res.json();
      if (data.ok) {
        // Verified! Save phone
        localStorage.setItem('aegis_user_phone', phone);
        localStorage.setItem('aegis_phone_verified', 'true');
        setProfile(prev => ({ ...prev, phone }));
        setPhoneVerified(true);
        setPhoneStep('idle');
        setPhoneStatus('');
        setPhoneError('');
      } else {
        setPhoneError(data.error || 'Verification failed');
        setPhoneStatus('');
      }
    } catch (err) {
      setPhoneError('Could not reach server.');
      setPhoneStatus('');
    }
  };

  const handleOtpChange = (index, value) => {
    // Only allow digits
    const digit = value.replace(/\D/g, '').slice(-1);
    const newDigits = [...otpDigits];
    newDigits[index] = digit;
    setOtpDigits(newDigits);

    // Auto-advance to next box
    if (digit && index < 5) {
      otpRefs.current[index + 1]?.focus();
    }
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
    if (e.key === 'Enter' && otpDigits.join('').length === 6) {
      handleVerifyOtp();
    }
  };

  const handleOtpPaste = (e) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length > 0) {
      e.preventDefault();
      const newDigits = [...otpDigits];
      for (let i = 0; i < 6; i++) {
        newDigits[i] = pasted[i] || '';
      }
      setOtpDigits(newDigits);
      const focusIdx = Math.min(pasted.length, 5);
      otpRefs.current[focusIdx]?.focus();
    }
  };

  const handlePhoneCancel = () => {
    setPhoneInput(profile.phone);
    setPhoneStep('idle');
    setPhoneError('');
    setPhoneStatus('');
    setOtpDigits(['', '', '', '', '', '']);
    if (cooldownRef.current) clearInterval(cooldownRef.current);
    setResendCooldown(0);
  };

  const handleRemovePhone = () => {
    localStorage.removeItem('aegis_user_phone');
    localStorage.removeItem('aegis_phone_verified');
    setProfile(prev => ({ ...prev, phone: '' }));
    setPhoneInput('');
    setPhoneVerified(false);
  };

  const toggleSetting = (key) => setToggles(p => {
    const updated = { ...p, [key]: !p[key] };
    localStorage.setItem('aegis_smart_alerts', JSON.stringify(updated));
    return updated;
  });

  // Shared styles
  const btnPrimary = {
    padding: '6px 16px',
    borderRadius: '8px',
    border: 'none',
    background: 'var(--accent-primary)',
    color: '#fff',
    fontWeight: 600,
    fontSize: '13px',
    cursor: 'pointer',
    transition: 'opacity 0.2s',
  };
  const btnGhost = {
    padding: '6px 16px',
    borderRadius: '8px',
    border: '1px solid rgba(255,255,255,0.15)',
    background: 'transparent',
    color: 'var(--text-muted)',
    fontWeight: 500,
    fontSize: '13px',
    cursor: 'pointer',
  };
  const inputStyle = {
    flex: 1,
    padding: '8px 12px',
    borderRadius: '8px',
    border: '1px solid var(--accent-primary)',
    background: 'rgba(255,255,255,0.05)',
    color: 'var(--text-main)',
    fontSize: '14px',
    outline: 'none',
  };

  return (
    <div>
      <h1 style={{ marginBottom: '8px' }}>Hi {displayUsername}! Welcome Home</h1>
      <p style={{ marginBottom: '40px' }}>Your personal security and awareness overview.</p>
      
      <div className="dashboard-grid">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px' }}>
            <h2 className="card-title">Profile</h2>
            <div style={{ background: 'var(--accent-primary)', color: 'white', width: '32px', height: '32px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>!</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: '#ccc', margin: '0 auto 16px', backgroundImage: `url("https://api.dicebear.com/7.x/initials/svg?seed=${displayUsername}&backgroundColor=10b981")`, backgroundSize: 'cover' }}></div>
            <div style={{ fontSize: '18px', fontWeight: 600 }}>{displayUsername}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: '14px', marginTop: '4px' }}>
              {emailLocal ? `${emailLocal}@…` : 'No email linked'}
            </div>
          </div>
        </div>

        <div className="card" style={{ gridColumn: 'span 2' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px' }}>
            <h2 className="card-title" style={{ margin: 0 }}>Awareness Rank Profile</h2>
            <div style={{ fontSize: '24px', fontWeight: 700, color: currentRank.color }}>{hasHistory ? currentRank.name : 'Unknown'}</div>
          </div>
          
          {!hasHistory ? (
            <div style={{ height: '140px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
              Complete the Training Simulation to establish your rank.
            </div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', height: '140px', padding: '20px', background: `${currentRank.color}15`, borderRadius: '16px', border: `1px solid ${currentRank.color}40` }}>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <h3 style={{ color: currentRank.color, margin: 0, fontSize: '20px' }}>Rank: {currentRank.name}</h3>
                  <span style={{ fontSize: '18px', fontWeight: 'bold', color: 'var(--text-main)' }}>{xp} XP</span>
                </div>
                <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.5 }}>
                  You have answered <strong>{history.filter(h => h.correct).length}</strong> scenarios correctly out of {history.length} attempts.
                  Your rank reflects your cumulative XP and overall ability to detect social engineering.
                </p>
              </div>
            </div>
          )}
        </div>

        <div className="card">
          <h2 className="card-title">Personal Information</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="list-item" style={{ background: 'rgba(255,255,255,0.03)', padding: '12px 16px', borderRadius: '12px' }}>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>User ID</div>
                <div style={{ fontWeight: 500, marginTop: '4px', color: 'var(--text-main)' }}>{profile.userId}</div>
              </div>
            </div>

            {/* ─── Phone Verification Section ─── */}
            <div className="list-item" style={{ background: 'rgba(255,255,255,0.03)', padding: '14px 16px', borderRadius: '12px', flexDirection: 'column', alignItems: 'stretch' }}>
              <div style={{ width: '100%' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  Phone
                  {phoneVerified && profile.phone && (
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', gap: '3px',
                      fontSize: '11px', fontWeight: 600,
                      color: '#10b981', background: 'rgba(16,185,129,0.12)',
                      padding: '2px 8px', borderRadius: '20px',
                    }}>
                      ✓ Verified
                    </span>
                  )}
                </div>

                {/* ── Step: Idle (show saved phone or "Add" prompt) ── */}
                {phoneStep === 'idle' && (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ fontWeight: 500, color: profile.phone ? 'var(--text-main)' : 'var(--text-muted)', fontStyle: profile.phone ? 'normal' : 'italic' }}>
                      {profile.phone || 'Add your phone number'}
                    </div>
                    <div style={{ display: 'flex', gap: '4px' }}>
                      {profile.phone && phoneVerified && (
                        <button
                          onClick={handleRemovePhone}
                          title="Remove phone number"
                          style={{ ...btnGhost, padding: '4px 8px', fontSize: '12px', color: '#ef4444', border: 'none' }}
                        >
                          ✕
                        </button>
                      )}
                      <button
                        onClick={() => { setPhoneStep('input'); setPhoneInput(profile.phone || ''); setPhoneError(''); setPhoneStatus(''); }}
                        title={profile.phone ? "Change phone number" : "Add phone number"}
                        style={{
                          background: 'none', border: 'none',
                          color: 'var(--accent-primary)', cursor: 'pointer',
                          fontSize: '14px', padding: '4px 8px', borderRadius: '6px',
                          transition: 'background 0.2s',
                        }}
                        onMouseEnter={(e) => e.target.style.background = 'rgba(255,107,0,0.12)'}
                        onMouseLeave={(e) => e.target.style.background = 'none'}
                      >
                        ✏️
                      </button>
                    </div>
                  </div>
                )}

                {/* ── Step: Phone Input ── */}
                {phoneStep === 'input' && (
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                      <input
                        type="tel"
                        value={phoneInput}
                        onChange={(e) => setPhoneInput(e.target.value)}
                        placeholder="e.g. +91 98765 43210"
                        autoFocus
                        onKeyDown={(e) => { if (e.key === 'Enter') handleSendOtp(); if (e.key === 'Escape') handlePhoneCancel(); }}
                        style={inputStyle}
                      />
                      <button
                        onClick={handleSendOtp}
                        disabled={!!phoneStatus}
                        style={{ ...btnPrimary, opacity: phoneStatus ? 0.6 : 1, whiteSpace: 'nowrap' }}
                      >
                        {phoneStatus ? '...' : 'Send OTP'}
                      </button>
                      <button onClick={handlePhoneCancel} style={btnGhost}>Cancel</button>
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                      We'll send a 6-digit verification code to this number.
                    </div>
                  </div>
                )}

                {/* ── Step: OTP Entry ── */}
                {phoneStep === 'otp' && (
                  <div>
                    <div style={{ fontSize: '13px', color: 'var(--text-main)', marginBottom: '12px' }}>
                      Enter the 6-digit code sent to <strong>{phoneInput}</strong>
                    </div>
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', justifyContent: 'center' }}>
                      {otpDigits.map((digit, i) => (
                        <input
                          key={i}
                          ref={el => otpRefs.current[i] = el}
                          type="text"
                          inputMode="numeric"
                          maxLength={1}
                          value={digit}
                          onChange={(e) => handleOtpChange(i, e.target.value)}
                          onKeyDown={(e) => handleOtpKeyDown(i, e)}
                          onPaste={i === 0 ? handleOtpPaste : undefined}
                          style={{
                            width: '42px', height: '48px',
                            textAlign: 'center', fontSize: '20px', fontWeight: 700,
                            borderRadius: '10px',
                            border: digit ? '2px solid var(--accent-primary)' : '1.5px solid rgba(255,255,255,0.15)',
                            background: digit ? 'rgba(255,107,0,0.08)' : 'rgba(255,255,255,0.04)',
                            color: 'var(--text-main)',
                            outline: 'none',
                            transition: 'border 0.2s, background 0.2s',
                            caretColor: 'var(--accent-primary)',
                          }}
                          onFocus={(e) => e.target.style.border = '2px solid var(--accent-primary)'}
                          onBlur={(e) => { if (!digit) e.target.style.border = '1.5px solid rgba(255,255,255,0.15)'; }}
                        />
                      ))}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}>
                      <button
                        onClick={handleVerifyOtp}
                        disabled={otpDigits.join('').length < 6 || !!phoneStatus}
                        style={{ ...btnPrimary, padding: '8px 28px', fontSize: '14px', opacity: (otpDigits.join('').length < 6 || phoneStatus) ? 0.5 : 1 }}
                      >
                        {phoneStatus === 'Verifying...' ? 'Verifying...' : 'Verify'}
                      </button>
                      <button onClick={handlePhoneCancel} style={btnGhost}>Cancel</button>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '10px' }}>
                      {resendCooldown > 0 ? (
                        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                          Resend in {resendCooldown}s
                        </span>
                      ) : (
                        <button
                          onClick={handleSendOtp}
                          style={{ ...btnGhost, border: 'none', fontSize: '12px', color: 'var(--accent-primary)', textDecoration: 'underline', padding: '2px 4px' }}
                        >
                          Resend OTP
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {/* Error / Status messages */}
                {phoneError && (
                  <div style={{
                    marginTop: '8px', fontSize: '12px', color: '#ef4444',
                    background: 'rgba(239,68,68,0.08)', padding: '6px 10px',
                    borderRadius: '8px', borderLeft: '3px solid #ef4444',
                  }}>
                    {phoneError}
                  </div>
                )}
                {phoneStatus && !phoneError && phoneStep === 'otp' && (
                  <div style={{
                    marginTop: '8px', fontSize: '12px', color: '#f59e0b',
                    background: 'rgba(245,158,11,0.08)', padding: '6px 10px',
                    borderRadius: '8px', borderLeft: '3px solid #f59e0b',
                  }}>
                    {phoneStatus}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="card" style={{ gridColumn: 'span 2' }}>
          <h2 className="card-title">Smart Alerts</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="list-item" style={{ border: 'none' }} onClick={() => toggleSetting('sms')}>
              <span style={{ fontWeight: 500, color: 'var(--text-main)' }}>Auto-scan SMS</span>
              <div className={`toggle-switch ${toggles.sms ? 'on' : ''}`}></div>
            </div>
            <div className="list-item" style={{ border: 'none' }} onClick={() => toggleSetting('calls')}>
              <span style={{ fontWeight: 500, color: 'var(--text-main)' }}>Call recording AI</span>
              <div className={`toggle-switch ${toggles.calls ? 'on' : ''}`}></div>
            </div>
            <div className="list-item" style={{ border: 'none' }} onClick={() => toggleSetting('whatsapp')}>
              <span style={{ fontWeight: 500, color: 'var(--text-main)' }}>WhatsApp Watch</span>
              <div className={`toggle-switch ${toggles.whatsapp ? 'on' : ''}`}></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
