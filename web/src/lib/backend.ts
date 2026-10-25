import { supabase } from './supabase';
import type { User as SupabaseUser } from '@supabase/supabase-js';

export type BackendUser = SupabaseUser;

export type BackendClient = typeof supabase;

export const backend: BackendClient = supabase;
