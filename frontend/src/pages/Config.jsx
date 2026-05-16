import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Config() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);

  const titles = ['Allow SMS Access', 'Allow Call Audios', 'Allow Email Access'];
  const descriptions = [
    'Aegis AI needs access to your SMS to automatically detect and flag phishing messages in real-time.',
    'We use local speech-to-text to analyze incoming suspicious calls for impersonation tactics.',
    'Connect your inbox to protect against spear-phishing and job-scam emails securely.',
  ];

  const handleNext = () => {
    if (step < 3) setStep(step + 1);
    else navigate('/home');
  };

  return (
    <div
      className="config-page-enter"
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '20px',
      }}
    >
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
            <motionConfigShell />
          ))}
        </div>
      </motionConfigShell>
    </motionConfigShell>
  );
}
