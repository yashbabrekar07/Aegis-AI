import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Shield } from 'lucide-react';

export default function PrivacyPolicy() {
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
          <Shield size={28} color="#10b981" />
          <h1 style={{ fontSize: '32px', fontWeight: 800, color: '#fff', margin: 0 }}>
            Privacy Policy
          </h1>
        </div>
        <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '48px' }}>
          Last updated: June 2025
        </p>

        <Section title="1. About Aegis AI">
          Aegis AI is a <strong>free, open-source educational cybersecurity awareness platform</strong> designed
          to help users identify phishing, social engineering, and online scams. We are a student-led research project
          committed to digital safety education.
        </Section>

        <Section title="2. Information We Collect">
          <strong>Authentication data:</strong> When you sign up, your email and hashed password are securely
          managed by <a href="https://supabase.com/docs/guides/auth" target="_blank" rel="noreferrer" style={linkStyle}>Supabase Auth</a>.
          We never see or store your plaintext password.
          <br /><br />
          <strong>Scan data:</strong> Text or audio you submit for analysis is processed in real-time by our AI models
          and is <strong>not stored, logged, or persisted</strong> on any server after the response is returned.
          <br /><br />
          <strong>Gmail IMAP access:</strong> If you use the Inbox Sandbox feature, your Gmail App Password is sent
          directly to our backend over HTTPS, used once to fetch emails via IMAP, and is <strong>never stored</strong>.
          <br /><br />
          <strong>Local storage:</strong> Training progress, scan history, and preferences are stored
          in your browser's localStorage only. We have no access to this data.
        </Section>

        <Section title="3. How We Use Information">
          All data submitted through Aegis AI is used exclusively for real-time analysis and educational feedback.
          We do not sell, share, or monetize any user data. We do not use cookies for tracking.
        </Section>

        <Section title="4. Third-Party Services">
          <ul style={{ paddingLeft: '20px', margin: '8px 0', lineHeight: 1.8 }}>
            <li><strong>Supabase</strong> — Authentication and optional profile storage</li>
            <li><strong>Vercel</strong> — Frontend hosting</li>
            <li><strong>Render</strong> — Backend API hosting</li>
            <li><strong>Google Speech-to-Text</strong> — Audio transcription (processed in-memory only)</li>
          </ul>
          No analytics, advertising, or fingerprinting services are used.
        </Section>

        <Section title="5. Data Security">
          All communication is encrypted via HTTPS/TLS. Authentication tokens are managed securely by Supabase.
          We follow OWASP best practices for web security including CSP headers, HSTS, and XSS protection.
        </Section>

        <Section title="6. Your Rights">
          You may delete your account and all associated data at any time through Supabase.
          Since we don't store scan data, there is nothing to request deletion of beyond your account profile.
        </Section>

        <Section title="7. Children's Privacy">
          Aegis AI is an educational tool suitable for all ages. We do not knowingly collect personally
          identifiable information from children under 13 beyond what is needed for account creation.
        </Section>

        <Section title="8. Contact">
          For privacy questions, contact us at: <strong>aegisai.project@gmail.com</strong>
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

const linkStyle = { color: '#10b981', textDecoration: 'underline' };

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '36px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '12px' }}>{title}</h2>
      <div style={{ fontSize: '15px', lineHeight: 1.7, color: '#94a3b8' }}>{children}</div>
    </div>
  );
}
