Texture.all.filter(t=>t.name.includes('saw')).forEach(t=>{t.getOwnMaterial().uniforms.SHADE.value=false;});
