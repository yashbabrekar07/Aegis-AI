import { useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';

export default function TermsOfService() {
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
          <FileText size={28} color="#10b981" />
          <h1 style={{ fontSize: '32px', fontWeight: 800, color: '#fff', margin: 0 }}>
            Terms of Service
          </h1>
        </div>
        <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '48px' }}>
          Last updated: June 2025
        </p>

        <Section title="1. Educational Purpose">
          Aegis AI is an <strong>educational cybersecurity awareness platform</strong> developed as an academic
          research project. All features — including phishing detection, scam simulation training, and email
          scanning — are designed exclusively for educational and awareness purposes.
          <br /><br />
          <strong style={{ color: '#f59e0b' }}>
            This platform does NOT engage in, promote, or facilitate phishing, scamming, or any form of
            cybercrime. Simulated scenarios use defanged (non-functional) example URLs for training purposes only.
          </strong>
        </Section>

        <Section title="2. Acceptable Use">
          By using Aegis AI, you agree to:
          <ul style={{ paddingLeft: '20px', margin: '8px 0', lineHeight: 1.8 }}>
            <li>Use the platform solely for educational and personal security awareness purposes</li>
            <li>Not attempt to reverse-engineer the AI models for malicious purposes</li>
            <li>Not use the scan tools to analyze content you do not own or have permission to analyze</li>
            <li>Not impersonate others or create fraudulent accounts</li>
          </ul>
        </Section>

        <Section title="3. Service Description">
          Aegis AI provides:
          <ul style={{ paddingLeft: '20px', margin: '8px 0', lineHeight: 1.8 }}>
            <li><strong>Text & Audio Scanning:</strong> AI-powered analysis of messages for phishing indicators</li>
            <li><strong>Security Training:</strong> Interactive simulations to improve scam recognition skills</li>
            <li><strong>Inbox Sandbox:</strong> Optional Gmail integration for email phishing detection</li>
          </ul>
          All analysis is performed in real-time and results are not stored on our servers.
        </Section>

        <Section title="4. Disclaimer of Warranties">
          Aegis AI is provided "as is" without warranty of any kind. While our AI models are trained on
          real-world phishing data, <strong>no automated system is 100% accurate</strong>. Users should exercise
          their own judgment and not rely solely on Aegis AI for security decisions.
        </Section>

        <Section title="5. Limitation of Liability">
          Aegis AI and its contributors shall not be liable for any damages arising from the use or inability
          to use the platform, including but not limited to damages from security incidents, data loss, or
          incorrect classifications.
        </Section>

        <Section title="6. Open Source">
          Aegis AI is an open-source project. The source code is publicly available on GitHub for transparency
          and community review. We welcome contributions from the security research community.
        </Section>

        <Section title="7. Changes to Terms">
          We reserve the right to update these terms at any time. Continued use of the platform after changes
          constitutes acceptance of the new terms.
        </Section>

        <Section title="8. Contact">
          For questions about these terms, contact us at: <strong>aegisai.project@gmail.com</strong>
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
