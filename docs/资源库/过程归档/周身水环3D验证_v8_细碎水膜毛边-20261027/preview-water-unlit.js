Texture.all.filter(t=>t.name.includes('ragged_splash')||t.name==='broken_saw_flow.png').forEach(t=>{t.getOwnMaterial().uniforms.SHADE.value=false;});
