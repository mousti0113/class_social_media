import { Environment } from './environment';

export const environment: Environment = {
  production: true,

  // Backend API URL
  apiUrl: '?',

  // WebSocket URL
  wsUrl: '?',

  // Cloudinary Storage Configuration 
  cloudinary: {
    cloudName: 'duenbvoog',
    uploadPreset: 'classconnect_images',
    folder: 'classconnect',
  },

  // Upload Configuration
  uploadMaxSize: 5 * 1024 * 1024,
  imageMaxWidth: 1920,
  imageMaxHeight: 1920,
  allowedImageTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],

  // Paginazione
  defaultPageSize: 20,
  maxPageSize: 100,

  // Debounce & Timing
  searchDebounceMs: 300,
  typingIndicatorDebounceMs: 500,

  // WebSocket
  wsReconnectInterval: 5000,
  wsMaxReconnectAttempts: 10,

  // Toast Notifications
  toastDuration: 3000,
  toastPosition: 'top-right' as const,

  // Cache
  httpCacheTimeout: 5 * 60 * 1000,
};