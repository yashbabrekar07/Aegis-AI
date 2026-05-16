import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { supabase } from '../supabaseClient';
import { syncUserFromSession, saveUsername, getEmailLocalPart } from '../utils/userStorage';

export default function Config() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [username, setUsername] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [usernameError, setUsernameError] = useState('');

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (!session) return;
      syncUserFromSession(session);
      const email = session.user.email || '';
      setUserEmail(email);
      const existing = localStorage.getItem('aegis_user_username');
      const meta = session.user.user_metadata || {};
      const suggested =
        existing ||
        meta.username ||
        meta.preferred_username ||
        getEmailLocalPart(email);
      if (suggested) setUsername(suggested);
      if (localStorage.getItem('aegis_user_username')) setStep(1);
    });
  }, []);

  const titles = ['Allow SMS Access', 'Allow Call Audios', 'Allow Email Access'];
  const descriptions = [
    'Aegis AI needs access to your SMS to automatically detect and flag phishing messages in real-time.',
    'We use local speech-to-text to analyze incoming suspicious calls for impersonation tactics.',
    'Connect your inbox to protect against spear-phishing and job-scam emails securely.',
  ];

  const handleUsernameContinue = async () => {
    if (!saveUsername(username)) {
      setUsernameError('Please enter a username.');
      return;
    }
    setUsernameError('');
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session?.user?.id && typeof supabase.from === 'function') {
        supabase
          .from('profiles')
          .upsert({ id: session.user.id, username: username.trim() }, { onConflict: 'id' })
          .then(() => null)
          .catch(() => null);
      }
    });
    setStep(1);
  };

  const handleNext = () => {
    if (step < 3) setStep(step + 1);
    else navigate('/home');
  };

  const shellStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    padding: '20px',
  };

  if (step === 0) {
    return (
      <div className="config-page-enter" style={shellStyle}>
        <div
          className="card config-step-card"
          style={{
            width: '100%',
            maxWidth: '450px',
            textAlign: 'center',
            padding: 'clamp(24px, 5vw, 48px) clamp(16px, 4vw, 32px)',
          }}
        >
          <h2 style={{ fontSize: '24px', marginBottom: '8px' }}>Choose your username</h2>
          <p style={{ marginBottom: '24px', lineHeight: 1.6, color: 'var(--text-muted)', fontSize: '14px' }}>
            This is shown on your profile dashboard
            {userEmail ? ` · signed in as ${getEmailLocalPart(userEmail)}` : ''}.
          </p>
          <input
            type="text"
            className="input-field"
            placeholder="Your display username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            maxLength={32}
            style={{ marginBottom: '8px', textAlign: 'center' }}
          />
          {usernameError && (
            <p style={{ color: '#ef4444', fontSize: '13px', marginBottom: '12px' }}>{usernameError}</p>
          )}
          <button
            type="button"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '16px' }}
            onClick={handleUsernameContinue}
          >
            Continue
          </button>
        </div>
      </div>
    );
  }

  const permStep = step;

  return (
    <div className="config-page-enter" style={shellStyle}>
      <div
        className="card config-step-card"
        style={{
          width: '100%',
          maxWidth: '450px',
          textAlign: 'center',
          padding: 'clamp(24px, 5vw, 48px) clamp(16px, 4vw, 32px)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginBottom: '32px' }}>
          {[1, 2, 3].map((s) => (
            <div
              key={s}
              style={{
                flex: 1,
                height: '6px',
                borderRadius: '4px',
                background: s <= permStep ? 'var(--accent-primary)' : 'rgba(255,255,255,0.12)',
              }}
            />
          ))}
        </div>

        <h2 style={{ fontSize: '24px', marginBottom: '16px' }}>{titles[permStep - 1]}</h2>
        <p style={{ marginBottom: '40px', lineHeight: 1.6 }}>{descriptions[permStep - 1]}</p>

        <div style={{ display: 'flex', gap: '16px', flexDirection: 'column' }}>
          <button type="button" className="btn btn-primary" onClick={handleNext}>
            Grant Permission
          </button>
          <button
            type="button"
            className="btn"
            style={{ background: 'transparent', color: 'var(--text-muted)' }}
            onClick={handleNext}
          >
            Skip for now
          </button>
        </div>
      </div>

      <div style={{ marginTop: '40px', width: '100%', maxWidth: '450px', textAlign: 'center' }}>
        <p style={{ fontSize: '13px' }}>
          <strong>Manual Fallback:</strong> Even if you deny permissions, you can always paste text or upload audio files manually in the app later.
        </p>
      </div>
    </div>
  );
}
