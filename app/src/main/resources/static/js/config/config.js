// config.js

/**
 * Configuration file for defining global constants and environment-specific settings.
 * 
 * API_BASE_URL:
 * - Base URL for all API requests made from the frontend.
 * - Easily switchable for different environments (development, staging, production).
 * 
 * Example usage:
 *   fetch(`${API_BASE_URL}/api/appointments`)
 */

// Empty string = same origin as the page. The frontend is served by the same Spring Boot app,
// so this works on localhost and on the Cloud IDE proxy URL without editing it each session.
export const API_BASE_URL = "";
