import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Shield, BookOpen, Users, ExternalLink, Lock } from 'lucide-react';

export default function About() {
  const navigate = useNavigate();

  return (
    <div style={{
      minHeight: '100vh',
      background: '#000',
      color: '#e2e8f0',
      padding: 'clamp(24px, 5vw, 60px)',
      fontFamily: 'Inter, system-ui, sans-serif',
    }}>
      <div style={{ maxWidth: '780px', margin: '0 auto' }}>
        <button
          onClick={() => navigate(-1)}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            background: 'none', border: 'none', color: '#10b981',
            cursor: 'pointer', fontSize: '14px', fontWeight: 600,
            marginBottom: '32px', padding: 0,
          }}
        >
          <ArrowLeft size={18} /> Back
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <Shield size={32} color="#10b981" />
          <h1 style={{ fontSize: '32px', fontWeight: 800, color: '#fff', margin: 0 }}>
            About Aegis AI
          </h1>
        </div>
        <p style={{ color: '#64748b', fontSize: '16px', marginBottom: '48px', lineHeight: 1.6 }}>
          An open-source educational platform for cybersecurity awareness and digital safety training.
        </p>

        {/* Mission Card */}
        <div style={{
          background: 'rgba(16, 185, 129, 0.06)',
          border: '1px solid rgba(16, 185, 129, 0.15)',
          borderRadius: '16px', padding: '32px', marginBottom: '36px',
        }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#10b981', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <BookOpen size={22} /> Our Mission
          </h2>
          <p style={{ fontSize: '15px', lineHeight: 1.8, color: '#94a3b8', margin: 0 }}>
            Aegis AI was created to <strong style={{ color: '#e2e8f0' }}>empower everyday users</strong> to
            recognize and defend against online scams, phishing attacks, and social engineering. In a world
            where digital threats evolve daily, we believe that <strong style={{ color: '#e2e8f0' }}>awareness
            is the strongest defense</strong>.
            <br /><br />
            Our platform uses machine learning models trained on real-world datasets to analyze suspicious
            content in real-time, and provides interactive training simulations to build practical
            scam-detection instincts.
          </p>
        </div>

        {/* What We Do */}
        <Section title="What Aegis AI Does">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginTop: '16px' }}>
            <FeatureCard
              icon="🔍"
              title="Scan & Detect"
              desc="Analyze text messages, emails, and audio for social engineering indicators using NLP and ML models."
            />
            <FeatureCard
              icon="🎓"
              title="Train & Learn"
              desc="Interactive phishing simulations based on real-world scam patterns from India and globally."
            />
            <FeatureCard
              icon="📧"
              title="Email Analysis"
              desc="Connect your Gmail inbox to automatically scan for suspicious emails using AI classification."
            />
          </div>
        </Section>

        {/* What We Are NOT */}
        <div style={{
          background: 'rgba(245, 158, 11, 0.06)',
          border: '1px solid rgba(245, 158, 11, 0.15)',
          borderRadius: '16px', padding: '32px', marginBottom: '36px',
        }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#f59e0b', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Lock size={22} /> Important Disclaimer
          </h2>
          <p style={{ fontSize: '15px', lineHeight: 1.8, color: '#94a3b8', margin: 0 }}>
            Aegis AI is <strong style={{ color: '#e2e8f0' }}>strictly an educational and defensive tool</strong>.
            We do <strong style={{ color: '#ef4444' }}>NOT</strong> engage in, promote, or facilitate any form of
            phishing, scamming, or cybercrime. All simulated scenarios in our training module use defanged
            (non-functional) example URLs and are designed solely to teach users how to recognize threats.
            <br /><br />
            Our training content is similar to cybersecurity awareness programs used by organizations like
            SANS Institute, KnowBe4, and Google's Phishing Quiz.
          </p>
        </div>

        {/* Technology */}
        <Section title="Technology Stack">
          <ul style={{ paddingLeft: '20px', margin: '8px 0', lineHeight: 2 }}>
            <li><strong>Frontend:</strong> React + Vite, hosted on Vercel</li>
            <li><strong>Backend:</strong> Python FastAPI with scikit-learn ML models, hosted on Render</li>
            <li><strong>Authentication:</strong> Supabase Auth (OAuth + Email verification)</li>
            <li><strong>ML Models:</strong> Custom-trained NLP classifier on phishing/scam dataset</li>
            <li><strong>Speech:</strong> Google Speech-to-Text + OpenAI Whisper for audio analysis</li>
          </ul>
        </Section>

        {/* Team */}
        <Section title="The Team">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
            <Users size={20} color="#10b981" />
            <span style={{ fontSize: '15px', color: '#94a3b8' }}>
              Aegis AI is developed by a team of computer science students passionate about making
              cybersecurity education accessible to everyone.
            </span>
          </div>
        </Section>

        {/* Open Source */}
        <Section title="Open Source">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Github size={20} color="#94a3b8" />
            <span style={{ fontSize: '15px', color: '#94a3b8' }}>
              The full source code is publicly available on{' '}
              <a href="https://github.com/ayushmore007/Aegis-AI" target="_blank" rel="noreferrer" style={{ color: '#10b981', textDecoration: 'underline' }}>
                GitHub
              </a>
              {' '}for transparency and community review.
            </span>
          </div>
        </Section>

        <div style={{
          marginTop: '48px', paddingTop: '24px',
          borderTop: '1px solid rgba(255,255,255,0.08)',
          fontSize: '13px', color: '#475569', textAlign: 'center',
        }}>
          © {new Date().getFullYear()} Aegis AI — Open Source Educational Project
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '36px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '12px' }}>{title}</h2>
      <div style={{ fontSize: '15px', lineHeight: 1.7, color: '#94a3b8' }}>{children}</div>
    </div>
  );
}

function FeatureCard({ icon, title, desc }) {
  return (
    <div style={{
      background: 'rgba(255,255,255,0.03)',
      border: '1px solid rgba(255,255,255,0.06)',
      borderRadius: '12px', padding: '20px',
    }}>
      <div style={{ fontSize: '28px', marginBottom: '12px' }}>{icon}</div>
      <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#fff', marginBottom: '8px' }}>{title}</h3>
      <p style={{ fontSize: '13px', lineHeight: 1.6, color: '#64748b', margin: 0 }}>{desc}</p>
    </div>
  );
}
