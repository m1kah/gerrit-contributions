/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * (Taken from JGit org.eclipse.jgit.pgm.opt.CmdLineParser.)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Git Development Community nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.gerrit.util.cli;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.IllegalAnnotationError;
import org.kohsuke.args4j.NamedOptionDef;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.kohsuke.args4j.spi.EnumOptionHandler;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Extended command line parser which handles --foo=value arguments.
 * <p>
 * The args4j package does not natively handle --foo=value and instead prefers
 * to see --foo value on the command line. Many users are used to the GNU style
 * --foo=value long option, so we convert from the GNU style format to the
 * args4j style format prior to invoking args4j for parsing.
 */
public class CmdLineParser {
  public interface Factory {
    CmdLineParser create(Object bean);
  }

  private final Injector injector;
  private final MyParser parser;

  /**
   * Creates a new command line owner that parses arguments/options and set them
   * into the given object.
   *
   * @param bean instance of a class annotated by
   *        {@link org.kohsuke.args4j.Option} and
   *        {@link org.kohsuke.args4j.Argument}. this object will receive
   *        values.
   *
   * @throws IllegalAnnotationError if the option bean class is using args4j
   *         annotations incorrectly.
   */
  @Inject
  public CmdLineParser(final Injector injector, @Assisted final Object bean)
      throws IllegalAnnotationError {
    this.injector = injector;
    this.parser = new MyParser(bean);
  }

  public void addArgument(Setter<?> setter, Argument a) {
    parser.addArgument(setter, a);
  }

  public void addOption(Setter<?> setter, Option o) {
    parser.addOption(setter, o);
  }

  public void printSingleLineUsage(Writer w, ResourceBundle rb) {
    parser.printSingleLineUsage(w, rb);
  }

  public void printUsage(Writer out, ResourceBundle rb) {
    parser.printUsage(out, rb);
  }

  public void printDetailedUsage(String name, StringWriter out) {
    out.write(name);
    printSingleLineUsage(out, null);
    out.write('\n');
    out.write('\n');
    printUsage(out, null);
    out.write('\n');
  }

  public void printQueryStringUsage(String name, StringWriter out) {
    out.write(name);

    char next = '?';
    List<NamedOptionDef> booleans = new ArrayList<NamedOptionDef>();
    for (@SuppressWarnings("rawtypes") OptionHandler handler : parser.options) {
      if (handler.option instanceof NamedOptionDef) {
        NamedOptionDef n = (NamedOptionDef) handler.option;

        if (handler instanceof BooleanOptionHandler) {
          booleans.add(n);
          continue;
        }

        if (!n.required()) {
          out.write('[');
        }
        out.write(next);
        next = '&';
        if (n.name().startsWith("--")) {
          out.write(n.name().substring(2));
        } else if (n.name().startsWith("-")) {
          out.write(n.name().substring(1));
        } else {
          out.write(n.name());
        }
        out.write('=');

        String var = handler.getDefaultMetaVariable();
        if (handler instanceof EnumOptionHandler) {
          var = var.substring(1, var.length() - 1);
          var = var.replaceAll(" ", "");
        }
        out.write(var);
        if (!n.required()) {
          out.write(']');
        }
        if (n.isMultiValued()) {
          out.write('*');
        }
      }
    }
    for (NamedOptionDef n : booleans) {
      if (!n.required()) {
        out.write('[');
      }
      out.write(next);
      next = '&';
      if (n.name().startsWith("--")) {
        out.write(n.name().substring(2));
      } else if (n.name().startsWith("-")) {
        out.write(n.name().substring(1));
      } else {
        out.write(n.name());
      }
      if (!n.required()) {
        out.write(']');
      }
    }
  }

  public boolean wasHelpRequestedByOption() {
    return parser.help.value;
  }

  public void parseArgument(final String... args) throws CmdLineException {
    final ArrayList<String> tmp = new ArrayList<String>(args.length);
    for (int argi = 0; argi < args.length; argi++) {
      final String str = args[argi];
      if (str.equals("--")) {
        while (argi < args.length)
          tmp.add(args[argi++]);
        break;
      }

      if (str.startsWith("--")) {
        final int eq = str.indexOf('=');
        if (eq > 0) {
          tmp.add(str.substring(0, eq));
          tmp.add(str.substring(eq + 1));
          continue;
        }
      }

      tmp.add(str);
    }
    parser.parseArgument(tmp.toArray(new String[tmp.size()]));
  }

  public void parseOptionMap(Map<String, String[]> parameters)
      throws CmdLineException {
    parseOptionMap(parameters, Collections.<String>emptySet());
  }

  public void parseOptionMap(Map<String, String[]> parameters,
      Set<String> argNames)
      throws CmdLineException {
    ArrayList<String> tmp = new ArrayList<String>();
    for (Map.Entry<String, String[]> ent : parameters.entrySet()) {
      String name = ent.getKey();
      if (!name.startsWith("-")) {
        if (name.length() == 1) {
          name = "-" + name;
        } else {
          name = "--" + name;
        }
      }

      if (findHandler(name) instanceof BooleanOptionHandler) {
        boolean on = false;
        for (String value : ent.getValue()) {
          on = toBoolean(ent.getKey(), value);
        }
        if (on) {
          tmp.add(name);
        }
      } else {
        for (String value : ent.getValue()) {
          if (!argNames.contains(ent.getKey())) {
            tmp.add(name);
          }
          tmp.add(value);
        }
      }
    }
    parser.parseArgument(tmp.toArray(new String[tmp.size()]));
  }

  @SuppressWarnings("rawtypes")
  private OptionHandler findHandler(String name) {
    for (OptionHandler handler : parser.options) {
      if (handler.option instanceof NamedOptionDef) {
        NamedOptionDef def = (NamedOptionDef) handler.option;
        if (name.equals(def.name())) {
          return handler;
        }
        for (String alias : def.aliases()) {
          if (name.equals(alias)) {
            return handler;
          }
        }
      }
    }
    return null;
  }

  private boolean toBoolean(String name, String value) throws CmdLineException {
    if ("true".equals(value) || "t".equals(value)
        || "yes".equals(value) || "y".equals(value)
        || "on".equals(value)
        || "1".equals(value)
        || value == null || "".equals(value)) {
      return true;
    }

    if ("false".equals(value) || "f".equals(value)
        || "no".equals(value) || "n".equals(value)
        || "off".equals(value)
        || "0".equals(value)) {
      return false;
    }

    throw new CmdLineException(parser, String.format(
        "invalid boolean \"%s=%s\"", name, value));
  }

  private class MyParser extends org.kohsuke.args4j.CmdLineParser {
    @SuppressWarnings("rawtypes")
    private List<OptionHandler> options;
    private HelpOption help;

    MyParser(final Object bean) {
      super(bean);
      ensureOptionsInitialized();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected OptionHandler createOptionHandler(final OptionDef option,
        final Setter setter) {
      if (isHandlerSpecified(option) || isEnum(setter) || isPrimitive(setter)) {
        return add(super.createOptionHandler(option, setter));
      }

      final Key<OptionHandlerFactory<?>> key =
          OptionHandlerUtil.keyFor(setter.getType());
      Injector i = injector;
      while (i != null) {
        if (i.getBindings().containsKey(key)) {
          return add(i.getInstance(key).create(this, option, setter));
        }
        i = i.getParent();
      }

      return add(super.createOptionHandler(option, setter));
    }

    @SuppressWarnings("rawtypes")
    private OptionHandler add(OptionHandler handler) {
      ensureOptionsInitialized();
      options.add(handler);
      return handler;
    }

    @SuppressWarnings("rawtypes")
    private void ensureOptionsInitialized() {
      if (options == null) {
        help = new HelpOption();
        options = new ArrayList<OptionHandler>();
        addOption(help, help);
      }
    }

    private boolean isHandlerSpecified(final OptionDef option) {
      return option.handler() != OptionHandler.class;
    }

    private <T> boolean isEnum(Setter<T> setter) {
      return Enum.class.isAssignableFrom(setter.getType());
    }

    private <T> boolean isPrimitive(Setter<T> setter) {
      return setter.getType().isPrimitive();
    }
  }

  private static class HelpOption implements Option, Setter<Boolean> {
    private boolean value;

    @Override
    public String name() {
      return "--help";
    }

    @Override
    public String[] aliases() {
      return new String[] {"-h"};
    }

    @Override
    public String usage() {
      return "display this help text";
    }

    @Override
    public void addValue(Boolean val) {
      value = val;
    }

    @Override
    public Class<? extends OptionHandler<Boolean>> handler() {
      return BooleanOptionHandler.class;
    }

    @Override
    public String metaVar() {
      return "";
    }

    @Override
    public boolean multiValued() {
      return false;
    }

    @Override
    public boolean required() {
      return false;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Option.class;
    }

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

    @Override
    public boolean isMultiValued() {
      return multiValued();
    }
  }
}
