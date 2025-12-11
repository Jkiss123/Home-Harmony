// Vercel Serverless Function - Email Proxy
// Allows Android app to send email via EmailJS

module.exports = async (req, res) => {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  try {
    const { service_id, template_id, user_id, template_params } = req.body;

    // Validate required fields
    if (!service_id || !template_id || !user_id || !template_params) {
      return res.status(400).json({
        error: 'Missing required fields',
        required: ['service_id', 'template_id', 'user_id', 'template_params']
      });
    }

    // Forward request to EmailJS API
    const response = await fetch('https://api.emailjs.com/api/v1.0/email/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        service_id,
        template_id,
        user_id,
        template_params
      })
    });

    if (response.ok) {
      return res.status(200).json({
        success: true,
        message: 'Email sent successfully'
      });
    } else {
      const errorText = await response.text();
      return res.status(response.status).json({
        error: 'Failed to send email',
        details: errorText
      });
    }

  } catch (error) {
    console.error('Error:', error);
    return res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
};
