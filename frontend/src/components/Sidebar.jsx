import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Shield, Home, Clock, Activity, Settings, LogOut, Mail, LayoutDashboard } from 'lucide-react';
import { supabase } from '../supabaseClient';
import { getStoredUsername, getStoredEmail, getEmailLocalPart } from '../utils/userStorage';

export default function Sidebar() {
  const [expanded, setExpanded] = useState(false);
  const displayUsername = getStoredUsername();
  const emailLocal = getEmailLocalPart(getStoredEmail());

  return (
    <nav className={`sidebar ${expanded ? 'expanded' : ''}`} onMouseEnter={() => setExpanded(true)} onMouseLeave={() => setExpanded(false)}>
      <div className="sidebar-top">
        <div className="sidebar-logo">
          <Shield size={32} />
        </div>
        <span className="nav-text title-text">
          <span className="gradient-text">Aegis AI</span>
        </span>
      </div>

      <div className="sidebar-links">
        <NavLink to="/home" className={({ isActive }) => `nav-item ${isActive ? 'active-primary' : ''}`} title="Scan">
          <div className="nav-icon"><Home size={22} /></div>
          <span className="nav-text">Scan Center</span>
        </NavLink>
        <NavLink to="/email" className={({ isActive }) => `nav-item ${isActive ? 'active-primary' : ''}`} title="Inbox">
          <div className="nav-icon"><Mail size={22} /></div>
          <span className="nav-text">Inbox Sandbox</span>
        </NavLink>
        <NavLink to="/history" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} title="Logs">
          <div className="nav-icon"><Clock size={22} /></div>
          <span className="nav-text">Scan Logs</span>
        </NavLink>
        <NavLink to="/simulate" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} title="Training">
          <div className="nav-icon"><Activity size={22} /></div>
          <span className="nav-text">Training Simulation</span>
        </NavLink>
        <NavLink to="/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} title="Dashboard">
          <div className="nav-icon"><LayoutDashboard size={22} /></div>
          <span className="nav-text">Profile Dashboard</span>
        </NavLink>
      </div>

      <div className="sidebar-bottom">
        <div className="sidebar-profile-bar nav-item">
          <div
            className="nav-icon sidebar-profile-avatar"
            style={{
              backgroundImage: `url("https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(displayUsername)}&backgroundColor=10b981")`,
            }}
          />
          <div className="nav-text sidebar-profile-text">
            <span className="sidebar-profile-name">{displayUsername}</span>
            <span className="sidebar-profile-email">{emailLocal ? `${emailLocal}@…` : '—'}</span>
          </div>
        </div>
        <div onClick={() => supabase.auth.signOut()} className="nav-item logout-btn">
          <div className="nav-icon"><LogOut size={22} /></div>
          <span className="nav-text">Log Out</span>
        </div>
      </div>
    </nav>
  );
}
