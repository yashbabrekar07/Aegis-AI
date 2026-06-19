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
          An educational cybersecurity awareness platform. Learn to identify online threats through interactive training and AI-powered analysis.
        </p>

        {/* Educational disclaimer */}
        <p style={{ fontSize: '13px', maxWidth: '500px', margin: '0 auto 40px', color: '#475569', lineHeight: 1.6 }}>
          Aegis AI is a free, open-source educational tool developed by students.
          We do not collect, store, or share any personal data.
        </p>
      
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: '10px' }}>
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

      {/* Trust footer with legal links */}
      <div style={{
        position: 'absolute', bottom: '24px', left: 0, right: 0,
        display: 'flex', justifyContent: 'center', gap: '24px',
        fontSize: '13px', color: '#475569',
      }}>
        <span
          onClick={() => navigate('/about')}
          style={{ cursor: 'pointer', transition: 'color 0.2s' }}
          onMouseEnter={(e) => e.target.style.color = '#10b981'}
          onMouseLeave={(e) => e.target.style.color = '#475569'}
        >About</span>
        <span
          onClick={() => navigate('/privacy')}
          style={{ cursor: 'pointer', transition: 'color 0.2s' }}
          onMouseEnter={(e) => e.target.style.color = '#10b981'}
          onMouseLeave={(e) => e.target.style.color = '#475569'}
        >Privacy Policy</span>
        <span
          onClick={() => navigate('/terms')}
          style={{ cursor: 'pointer', transition: 'color 0.2s' }}
          onMouseEnter={(e) => e.target.style.color = '#10b981'}
          onMouseLeave={(e) => e.target.style.color = '#475569'}
        >Terms of Service</span>
      </div>
    </div>
  );
}
