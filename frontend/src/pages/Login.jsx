import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { Shield } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { saveUsername } from '../utils/userStorage';
import appleLogo from '../assets/apple-sign-in.svg';
import '../styles/login.css';

export default function Login({ isSignup }) {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Auto redirect if already logged in (OAuth returns to /config; email login goes there too)
  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session) navigate('/config');
    });
  }, [navigate]);

  const handleOAuthLogin = async (provider) => {
    setLoading(true);
    setErrorMsg('');
    const { error } = await supabase.auth.signInWithOAuth({
      provider: provider,
      options: {
        redirectTo: window.location.origin + '/config'
      }
    });
    if (error) setErrorMsg(error.message);
    setLoading(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setLoading(true);

    if (isSignup) {
      const { data, error } = await supabase.auth.signUp({
        email: email,
        password: password,
        options: {
          data: {
            first_name: firstName,
            last_name: lastName,
            username: username,
          }
        }
      });

      if (error) {
        setErrorMsg(error.message);
      } else {
        if (email.trim()) localStorage.setItem('aegis_user_email', email.trim());
        saveUsername(username);
        setSuccessMsg('Account created successfully! Please check your email inbox to confirm your email address before logging in.');
      }
    } else {
      const { data, error } = await supabase.auth.signInWithPassword({
        email: email,
        password: password,
      });

      if (error) {
        setErrorMsg(error.message);
      } else {
        if (email.trim()) localStorage.setItem('aegis_user_email', email.trim());
        const userMeta = data.user?.user_metadata;
        if (userMeta?.username) {
          saveUsername(userMeta.username);
        } else if (userMeta?.first_name) {
          saveUsername(`${userMeta.first_name} ${userMeta.last_name || ''}`.trim());
        } else if (email.trim()) {
          const derived = email.split('@')[0];
          saveUsername(derived.charAt(0).toUpperCase() + derived.slice(1));
        }

        // Ensure profile is inserted
        const username_from_meta = userMeta?.username || email.split('@')[0];
        if (data.user) {
          await supabase.from('profiles').insert([{ id: data.user.id, username: username_from_meta }]).catch(() => null);
        }

        navigate('/config');
      }
    }
    setLoading(false);
  };

  return (
    <div className="login-root">

      {/* Left Branding Panel - Dribbble Style */}
      <div className="login-brand-panel login-hide-mobile">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'white', zIndex: 10 }}>
          <Shield size={36} />
          <span style={{ fontSize: '26px', fontWeight: '800', letterSpacing: '-0.5px' }}>Aegis AI</span>
        </div>

        <div style={{ color: 'white', zIndex: 10, maxWidth: '500px', marginBottom: '10%' }}>
          <h2 style={{ fontSize: '48px', fontWeight: '800', lineHeight: 1.1, marginBottom: '24px', letterSpacing: '-1px' }}>
            Detect. Prevent.<br />Secure.
          </h2>
          <p style={{ fontSize: '18px', opacity: 0.9, lineHeight: 1.5, fontWeight: 400 }}>
            Advanced multi-channel phishing detection powered by real-time artificial intelligence. Join thousands protecting their digital footprint today.
          </p>
        </div>

        <div className="login-orb login-orb-a" />
        <div className="login-orb login-orb-b" />
      </div>

      <div className="login-form-panel">

        <div style={{ position: 'absolute', top: '40px', right: '40px', fontSize: '14px' }}>
          {isSignup ? "Already a member? " : "Not a member? "}
          <span
            style={{ color: '#10b981', cursor: 'pointer', fontWeight: 600, textDecoration: 'underline' }}
            onClick={() => navigate(isSignup ? '/login' : '/signup')}
          >
            {isSignup ? "Sign In" : "Sign up now"}
          </span>
        </div>

        <div className="login-form-inner">
          <h1 style={{ fontSize: '32px', fontWeight: '800', marginBottom: '32px', color: '#fff', letterSpacing: '-0.5px' }}>
            {isSignup ? "Sign up to Aegis" : "Sign in to Aegis"}
          </h1>

          {/* OAuth Buttons */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
            <button type="button" onClick={() => handleOAuthLogin('google')} className="login-social-btn login-oauth-btn">
              <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="G" style={{ width: '20px' }} />
              Sign {isSignup ? 'up' : 'in'} with Google
            </button>
            <button type="button" onClick={() => handleOAuthLogin('apple')} className="login-social-btn login-social-btn apple login-oauth-btn">
              <img src={appleLogo} alt="" className="apple-logo-img" aria-hidden="true" />
              Sign {isSignup ? 'up' : 'in'} with Apple
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', margin: '32px 0', color: '#666' }}>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
            <span style={{ padding: '0 16px', fontSize: '13px', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px' }}>
              or sign in with email
            </span>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
          </div>

          {/* Error/Success Messages */}
          {errorMsg && <div style={{ color: '#ef4444', marginBottom: '24px', fontSize: '14px', background: 'rgba(239, 68, 68, 0.1)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>{errorMsg}</div>}
          {successMsg && <div style={{ color: '#10b981', marginBottom: '24px', fontSize: '14px', background: 'rgba(16, 185, 129, 0.1)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(16, 185, 129, 0.2)' }}>{successMsg}</div>}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {isSignup && (
              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label className="label-text login-field">First Name</label>
                  <input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="login-input-dribbble login-field"
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label className="label-text login-field">Last Name</label>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="login-input-dribbble login-field"
                  />
                </div>
              </div>
            )}

            {isSignup && (
              <div>
                <label className="label-text login-field">Username</label>
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="login-input-dribbble login-field"
                />
              </div>
            )}

            <div>
              <label className="label-text login-field">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="login-input-dribbble login-field"
              />
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <label className="label-text login-field">Password</label>
                {!isSignup && (
                  <span style={{ fontSize: '13px', color: '#10b981', cursor: 'pointer', fontWeight: 500 }}>
                    Forgot password?
                  </span>
                )}
              </div>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="login-input-dribbble login-field"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="login-submit-btn login-field"
              style={{
                marginTop: '12px',
                background: 'linear-gradient(135deg, #10b981, #059669)',
                color: 'white',
                border: 'none',
                padding: '18px',
                borderRadius: '30px',
                fontSize: '16px',
                fontWeight: '700',
                cursor: loading ? 'wait' : 'pointer',
                transition: 'all 0.25s ease',
                opacity: loading ? 0.7 : 1,
                boxShadow: '0 8px 20px rgba(16, 185, 129, 0.25)',
                width: '100%',
              }}
            >
              {loading ? 'Please wait...' : isSignup ? 'Create account' : 'Sign In'}
            </button>
          </form>

        </div>
      </div>
    </div>
  );
}
