import { supabase } from '../supabaseClient';

/** Local part of email (e.g. ayush from ayush@gmail.com) */
export function getEmailLocalPart(email) {
  if (!email || !email.includes('@')) return '';
  return email.split('@')[0];
}

export function getStoredUsername() {
  return (
    localStorage.getItem('aegis_user_username') ||
    localStorage.getItem('aegis_user_name') ||
    'Guest'
  );
}

export function getStoredEmail() {
  return localStorage.getItem('aegis_user_email') || '';
}

/** Persist email (and optional username) from Supabase session after OAuth / login */
export async function syncUserFromSession(session) {
  if (!session?.user) return;

  const email = session.user.email || '';
  if (email) localStorage.setItem('aegis_user_email', email);

  const meta = session.user.user_metadata || {};
  const suggested =
    meta.username ||
    meta.preferred_username ||
    meta.full_name ||
    (meta.first_name ? `${meta.first_name} ${meta.last_name || ''}`.trim() : '') ||
    getEmailLocalPart(email);

  if (suggested && !localStorage.getItem('aegis_user_username')) {
    localStorage.setItem('aegis_user_name', suggested.trim());
  }

  if (email && session.user.id) {
    const username =
      localStorage.getItem('aegis_user_username') ||
      meta.username ||
      getEmailLocalPart(email);
    await supabase
      .from('profiles')
      .upsert({ id: session.user.id, username }, { onConflict: 'id' })
      .catch(() => null);
  }
}

export function saveUsername(username) {
  const trimmed = (username || '').trim();
  if (!trimmed) return false;
  localStorage.setItem('aegis_user_username', trimmed);
  localStorage.setItem('aegis_user_name', trimmed);
  return true;
}
