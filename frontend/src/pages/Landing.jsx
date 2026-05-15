import { useNavigate } from 'react-router-dom';
import { Fingerprint } from 'lucide-react';

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div style={{ 
      display: 'flex', flexDirection: 'column', alignItems: 'center', 
      justifyContent: 'center', minHeight: '100vh', textAlign: 'center',
      padding: '24px', background: '#000'
    }}>
      <style>
        {`
          .landing-title {
            font-size: clamp(48px, 10vw, 100px);
            font-weight: 800;
            margin-bottom: 24px;
            line-height: 1.1;
            letter-spacing: -3px;
          }
          .landing-icon {
            width: clamp(40px, 8vw, 80px);
            height: clamp(40px, 8vw, 80px);
          }
          @media (max-width: 600px) {
            .landing-title {
              letter-spacing: -1px;
            }
          }
        `}
      </style>
      
      <div style={{ maxWidth: '800px', margin: '0 auto', textAlign: 'center' }} className="animate-slide-up">
        <h1 className="landing-title">
          <span className="gradient-text dynamic-color-anim" style={{ display: 'inline-flex', alignItems: 'center', gap: '20px' }}>
            <Fingerprint className="landing-icon" color="#10b981" /> Aegis AI
          </span>
        </h1>
        
        <p style={{ fontSize: 'clamp(16px, 4vw, 22px)', maxWidth: '600px', margin: '40px auto', color: '#94a3b8', fontWeight: 400 }}>
          Detect scams. Train your instincts. Stay protected with the world's most advanced behavioral phishing guard.
        </p>
      
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: '50px' }}>
          <button 
            className="animate-pulse-glow"
            onClick={() => navigate('/login')}
            style={{ 
              background: '#0a0a0a', 
              color: '#fff', 
              border: '1px solid rgba(16, 185, 129, 0.4)',
              padding: '16px 40px', 
              borderRadius: '12px',
              fontSize: '18px',
              fontWeight: 500,
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
          >
            Join The Community
          </button>
        </div>
      </div>
    </div>
  );
}
