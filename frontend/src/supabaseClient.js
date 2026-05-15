import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

// Use a safe initialization to prevent "black screen" if credentials are missing
export const supabase = (supabaseUrl && supabaseUrl !== 'YOUR_SUPABASE_URL_HERE') 
  ? createClient(supabaseUrl, supabaseAnonKey) 
  : {
      auth: {
        getSession: async () => ({ data: { session: null }, error: null }),
        onAuthStateChange: (callback) => {
          // Store callback to trigger it manually for demo
          window.__supabase_auth_callback = callback;
          return { data: { subscription: { unsubscribe: () => {} } } };
        },
        signInDemo: () => {
          const mockSession = { user: { email: 'demo@aegis.ai', user_metadata: { first_name: 'Demo', last_name: 'User' } } };
          if (window.__supabase_auth_callback) {
            window.__supabase_auth_callback('SIGNED_IN', mockSession);
          }
          return { data: { session: mockSession }, error: null };
        },
        signInWithPassword: async () => ({ error: { message: "Supabase not configured. Use 'Demo Mode' to explore." } }),
        signUp: async () => ({ error: { message: "Supabase not configured. Use 'Demo Mode' to explore." } }),
        signInWithOAuth: async () => ({ error: { message: "Supabase not configured. Use 'Demo Mode' to explore." } }),
        signOut: async () => {
          if (window.__supabase_auth_callback) window.__supabase_auth_callback('SIGNED_OUT', null);
          return { error: null };
        }
      }
    };
