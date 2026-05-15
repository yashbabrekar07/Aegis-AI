import { useState, useEffect } from 'react';
import { getRankFromXp } from './Simulate';

export default function Dashboard() {
  const userName = localStorage.getItem('aegis_user_name') || 'Guest';
  const userEmail = localStorage.getItem('aegis_user_email') || 'Not provided';
  
  // Load rank from training xp
  const xp = parseInt(localStorage.getItem('aegis_training_xp')) || 0;
  const history = JSON.parse(localStorage.getItem('aegis_training_history')) || [];
  const currentRank = getRankFromXp(xp);
  
  const hasHistory = history.length > 0;

  const [toggles, setToggles] = useState({
    sms: true,
    calls: false,
    whatsapp: true
  });

  const [profile, setProfile] = useState({ userId: 'Loading...', phone: 'Loading...' });
  const [chartLoaded, setChartLoaded] = useState(false);

  useEffect(() => {
    fetch(`http://localhost:8000/api/user/profile?email=${encodeURIComponent(userEmail)}`)
      .then(res => res.json())
      .then(data => setProfile({ userId: data.user_id, phone: data.phone }))
      .catch(err => setProfile({ userId: 'Error', phone: 'Error' }));
      
    setTimeout(() => setChartLoaded(true), 100);
  }, [userEmail]);

  const toggleSetting = (key) => setToggles(p => ({ ...p, [key]: !p[key] }));

  return (
    <div>
      <h1 style={{ marginBottom: '8px' }}>Hi {userName}! Welcome Home</h1>
      <p style={{ marginBottom: '40px' }}>Your personal security and awareness overview.</p>
      
      <div className="dashboard-grid">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px' }}>
            <h2 className="card-title">Profile</h2>
            <div style={{ background: 'var(--accent-primary)', color: 'white', width: '32px', height: '32px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>!</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: '#ccc', margin: '0 auto 16px', backgroundImage: `url("https://api.dicebear.com/7.x/initials/svg?seed=${userName}&backgroundColor=10b981")`, backgroundSize: 'cover' }}></div>
            <div style={{ fontSize: '18px', fontWeight: 600 }}>{userName}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: '14px', marginTop: '4px' }}>{userEmail}</div>
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
            <div className="list-item" style={{ background: 'rgba(255,255,255,0.03)', padding: '12px 16px', borderRadius: '12px' }}>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Phone</div>
                <div style={{ fontWeight: 500, marginTop: '4px', color: 'var(--text-main)' }}>{profile.phone}</div>
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
