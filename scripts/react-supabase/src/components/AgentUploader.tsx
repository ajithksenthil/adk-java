import React from 'react';
import { supabase } from '../supabaseClient';

export default function AgentUploader() {
  const [file, setFile] = React.useState<File | null>(null);
  const [status, setStatus] = React.useState<string>('');

  const upload = async () => {
    if (!file) return;
    setStatus('Uploading...');
    const { data, error } = await supabase.storage
      .from('agent-source')
      .upload(file.name, file, { upsert: true });
    if (error) {
      setStatus(`Upload failed: ${error.message}`);
    } else {
      setStatus(`Uploaded ${data.path}`);
    }
  };

  return (
    <div>
      <h2>Upload Agent</h2>
      <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
      <button onClick={upload} disabled={!file}>Upload</button>
      <div>{status}</div>
    </div>
  );
}
