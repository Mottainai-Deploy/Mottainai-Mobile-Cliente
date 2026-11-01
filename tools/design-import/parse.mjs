// Development-only: translate the high-fidelity Figma context into a native view tree.
// No JSX, HTML, or remote Figma URLs are used at runtime.
import {parse} from '@babel/parser';
import fs from 'node:fs';
const contexts=JSON.parse(fs.readFileSync('.design-cache/contexts-latest.json','utf8'));
const output={};
const unknown=new Set();
for(const [id,source] of Object.entries(contexts)){
  try {
    const code=source.split('SUPER CRITICAL:')[0];
    const ast=parse(code,{sourceType:'module',plugins:['typescript','jsx']});
    const functions={},global={};let main;
    for(const st of ast.program.body){
      if(st.type==='FunctionDeclaration') functions[st.id.name]=st;
      if(st.type==='ExportDefaultDeclaration'){main=st.declaration;if(main.type==='FunctionDeclaration')functions[main.id.name]=main;}
      if(st.type==='VariableDeclaration')for(const d of st.declarations)global[d.id.name]=value(d.init,global);
    }
    function value(n,e){
      if(!n)return null;
      switch(n.type){
        case 'StringLiteral':case 'NumericLiteral':case 'BooleanLiteral':return n.value;
        case 'NullLiteral':return null;
        case 'Identifier':return e[n.name];
        case 'ParenthesizedExpression':case 'TSAsExpression':return value(n.expression,e);
        case 'LogicalExpression':{const a=value(n.left,e);return n.operator==='&&'?(a&&value(n.right,e)):(a||value(n.right,e));}
        case 'BinaryExpression':{const a=value(n.left,e),b=value(n.right,e);if(n.operator==='==='||n.operator==='==')return a===b;if(n.operator==='!==')return a!==b;if(n.operator==='+')return a+b;break;}
        case 'UnaryExpression':if(n.operator==='!')return !value(n.argument,e);break;
        case 'ConditionalExpression':return value(value(n.test,e)?n.consequent:n.alternate,e);
        case 'TemplateLiteral':return n.quasis.map((q,i)=>q.value.cooked+(i<n.expressions.length?(value(n.expressions[i],e)??''):'')).join('');
        case 'TaggedTemplateExpression':return value(n.quasi,e);
        case 'ObjectExpression':return Object.fromEntries(n.properties.filter(p=>p.type==='ObjectProperty').map(p=>[p.key.name||p.key.value,value(p.value,e)]));
        case 'JSXElement':return element(n,e);
        case 'JSXFragment':return n.children.map(c=>value(c,e)).flat().filter(Boolean);
        case 'JSXExpressionContainer':return value(n.expression,e);
        case 'JSXText':return n.value.replace(/\s+/g,' ').trim();
      }
      unknown.add(n.type);return null;
    }
    function invoke(fn,props){
      const e={...global,...props};const param=fn.params[0];
      if(param?.type==='ObjectPattern')for(const p of param.properties){const key=p.key.name;if(e[key]===undefined&&p.value.type==='AssignmentPattern')e[key]=value(p.value.right,e);}
      for(const st of fn.body.body){if(st.type==='VariableDeclaration')for(const d of st.declarations)e[d.id.name]=value(d.init,e);if(st.type==='ReturnStatement')return value(st.argument,e);}
      return null;
    }
    function element(n,e){
      const name=n.openingElement.name.name,attrs={};
      for(const a of n.openingElement.attributes){if(a.type==='JSXAttribute')attrs[a.name.name]=a.value?value(a.value,e):true;}
      if(functions[name])return invoke(functions[name],attrs);
      const children=n.children.map(c=>value(c,e)).flat().filter(c=>c!==null&&c!==false&&c!==undefined&&c!=='');
      return {type:name,attrs,children};
    }
    output[id]=invoke(main,{});
  }catch(err){console.error(id,err.message);process.exitCode=1;}
}
fs.writeFileSync('.design-cache/trees.json',JSON.stringify(output));
console.log(JSON.stringify({screens:Object.keys(output).length,unsupported:[...unknown]}));
